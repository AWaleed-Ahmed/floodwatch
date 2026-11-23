/*
 * File:    AuthService.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Checks logins, issues login tokens and resolves tokens back to users.
 */


package com.floodwatch.service;

import com.floodwatch.config.AppProperties;
import com.floodwatch.model.User;
import com.floodwatch.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;


/**
 * Checks logins, issues login tokens and turns a token back into its user.
 * <p>
 * Tokens are 32 random bytes from SecureRandom, Base64-URL encoded, and kept only in memory.
 * They are lost when the server restarts, so everyone has to log in again after a restart.
 * Each server keeps its own tokens, so a token from one server is unknown to another.
 * Expired tokens are removed when they are seen, and while a new token is issued.
 * <p>
 * A wrong password and an unknown username give the same error, and BCrypt runs in both cases,
 * so the caller cannot tell which usernames exist. Passwords are checked exactly as sent
 * (never trimmed) and must be at most 72 bytes, because BCrypt ignores anything after that.
 * <p>
 * All state lives in thread-safe maps, so concurrent logins and logouts cannot corrupt it.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Service
public class AuthService {


    private static final int TOKEN_BYTES = 32;
    private static final int MAX_PASSWORD_BYTES = 72;
    private static final String INVALID_LOGIN = "Invalid username or password.";
    private static final String TOO_MANY_LOGINS = "Too many failed logins. Try again in a minute.";
    private static final String PLACEHOLDER_PASSWORD = "placeholder-never-matches";

    private final UserRepository users;
    private final LoginAttemptLimiter limiter;
    private final Clock clock;
    private final Duration tokenTtl;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final String placeholderHash;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    /**
     * Creates the service.
     *
     * @param users  account storage
     * @param limiter failed-login counter
     * @param clock  source of the current time
     * @param props  application settings holding the token lifetime
     * @throws IllegalArgumentException when the token lifetime is less than one hour
     */


    public AuthService
    (
        UserRepository users,
        LoginAttemptLimiter limiter,
        Clock clock,
        AppProperties props
    ) {
        int hours = props.auth().tokenTtlHours();
        if (hours < 1)
        {
            throw new IllegalArgumentException("floodwatch.auth.token-ttl-hours must be at least 1");
        }
        this.users = users;
        this.limiter = limiter;
        this.clock = clock;
        this.tokenTtl = Duration.ofHours(hours);
        this.placeholderHash = encoder.encode(PLACEHOLDER_PASSWORD);
    }

    /**
     * Checks a username and password and, when both match, issues a new login token.
     * The username is trimmed and compared without case; the password is used exactly as sent.
     *
     * @param username the username typed by the caller
     * @param password the password typed by the caller
     * @return the new token, when it expires and the logged-in user
     * @throws BadRequestException      when the password is longer than 72 bytes
     * @throws TooManyRequestsException when this username has failed too often within the last minute
     * @throws UnauthorizedException    when the username or the password is wrong
     */


    public LoginResult login
    (
        String username, String password
    ) {
        String key = username.trim().toLowerCase(Locale.ROOT);
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES)
        {
            throw new BadRequestException("Password must be at most 72 bytes.");
        }
        if (limiter.isBlocked(key))
        {
            throw new TooManyRequestsException(TOO_MANY_LOGINS);
        }
        Optional<User> found = users.findByUsername(key);
        boolean matches = encoder.matches(password, found.map(User::passwordHash).orElse(placeholderHash));
        if (found.isEmpty() || !matches)
        {
            limiter.recordFailure(key);
            throw new UnauthorizedException(INVALID_LOGIN);
        }
        limiter.reset(key);
        return issueToken(found.get());
    }

    /**
     * Ends a login by deleting its token. A missing, unknown or expired token is ignored,
     * so logging out always succeeds.
     *
     * @param token the token to delete, may be null
     */


    public void logout
    (
        String token
    ) {
        if (token != null)
        {
            sessions.remove(token);
        }
    }

    /**
     * Finds the user who owns a token. An expired token is deleted the first time it is seen.
     * A token is valid up to and including its expiry instant.
     *
     * @param token the token sent by the caller, may be null
     * @return the user, or empty when the token is unknown, expired or its user no longer exists
     */


    public Optional<User> userForToken
    (
        String token
    ) {
        if (token == null)
        {
            return Optional.empty();
        }
        Session session = sessions.get(token);
        if (session == null)
        {
            return Optional.empty();
        }
        if (clock.instant().isAfter(session.expiresAt()))
        {
            sessions.remove(token, session);
            return Optional.empty();
        }
        return users.findById(session.userId());
    }

    /**
     * Creates and stores a new token for a user, after removing every token that has expired.
     *
     * @param user the user who just logged in
     * @return the login result holding the new token
     */


    private LoginResult issueToken
    (
        User user
    ) {
        Instant now = clock.instant();
        sessions.values().removeIf(session -> now.isAfter(session.expiresAt()));
        Instant expiresAt = now.plus(tokenTtl);
        String token = newToken();
        sessions.put(token, new Session(user.id(), expiresAt));
        return new LoginResult(token, expiresAt, user);
    }

    /**
     * Generates a random token.
     *
     * @return 32 random bytes, Base64-URL encoded without padding
     */


    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }


    /**
     * What a successful login returns.
     *
     * @param token     the token the caller sends back on later requests
     * @param expiresAt the last instant the token is accepted
     * @param user      the logged-in user
     * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
     */


    public record LoginResult
    (
        String token,
        Instant expiresAt,
        User user
    ) {


    }
}