/*
 * File:    AuthServiceTest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Tests login rules, the failed-login limit and token expiry with a controlled clock.
 */


package com.floodwatch.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.floodwatch.config.AppProperties;
import com.floodwatch.repository.LocalFileStorage;
import com.floodwatch.repository.UserRepository;
import com.floodwatch.support.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;


/**
 * Unit tests for AuthService with the seeded test accounts and a clock that only moves when a test says so.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


class AuthServiceTest {


    private static final String RESIDENT = "resident1";
    private static final String RESIDENT_PASSWORD = "resident1-demo";
    private static final String INVALID = "Invalid username or password.";
    private static final Instant START = Instant.parse("2026-11-23T10:00:00Z");
    private static final Duration TTL = Duration.ofHours(8);
    private static final int TOKEN_LENGTH = 43;

    private MutableClock clock;
    private AuthService auth;

    /**
     * Creates a fresh service for every test, with an empty failure count and no tokens.
     */


    @BeforeEach
    void setUp() {
        clock = new MutableClock(START);
        UserRepository users = new UserRepository(new LocalFileStorage("src/test/resources/test-data"), JsonMapper.builder().build());
        AppProperties props = new AppProperties(null, null, null, null, null, null, new AppProperties.Auth(8), null);
        auth = new AuthService(users, new LoginAttemptLimiter(clock), clock, props);
    }

    /**
     * A wrong password and an unknown username give the same error, so usernames cannot be guessed.
     */


    @Test
    void wrongPasswordAndUnknownUserGiveTheSameError() {
        UnauthorizedException wrongPassword = assertThrows(UnauthorizedException.class, () -> auth.login(RESIDENT, "wrong"));
        UnauthorizedException unknownUser = assertThrows(UnauthorizedException.class, () -> auth.login("nobody", "wrong"));
        assertEquals(INVALID, wrongPassword.getMessage());
        assertEquals(INVALID, unknownUser.getMessage());
        assertEquals(wrongPassword.getStatus(), unknownUser.getStatus());
    }

    /**
     * Surrounding spaces and letter case in the username are ignored.
     */


    @Test
    void usernameIsTrimmedAndIgnoresCase() {
        AuthService.LoginResult result = auth.login("  RESIDENT1 ", RESIDENT_PASSWORD);
        assertEquals("resident1", result.user().username());
    }

    /**
     * Passwords are compared exactly, so extra spaces make the password wrong.
     */


    @Test
    void passwordIsNotTrimmed() {
        assertThrows(UnauthorizedException.class, () -> auth.login(RESIDENT, RESIDENT_PASSWORD + " "));
        assertThrows(UnauthorizedException.class, () -> auth.login(RESIDENT, " " + RESIDENT_PASSWORD));
    }

    /**
     * A 73-byte password is refused with 400, and so is a short one with many bytes per character.
     */


    @Test
    void passwordLongerThan72BytesIsRejected() {
        assertThrows(BadRequestException.class, () -> auth.login(RESIDENT, "x".repeat(73)));
        assertThrows(BadRequestException.class, () -> auth.login(RESIDENT, "é".repeat(37)));
    }

    /**
     * A password of exactly 72 bytes is accepted for checking; it is only refused when it is wrong.
     */


    @Test
    void passwordOf72BytesIsCheckedNotRejected() {
        assertThrows(UnauthorizedException.class, () -> auth.login(RESIDENT, "x".repeat(72)));
    }

    /**
     * Five failures in one minute block the username, even with the correct password, until the minute ends.
     */


    @Test
    void fiveFailuresBlockTheUsernameUntilTheMinuteEnds() {
        for (int i = 0; i < 5; i++)
        {
            assertThrows(UnauthorizedException.class, () -> auth.login(RESIDENT, "wrong"));
        }
        assertThrows(TooManyRequestsException.class, () -> auth.login(RESIDENT, RESIDENT_PASSWORD));
        clock.advance(Duration.ofSeconds(59));
        assertThrows(TooManyRequestsException.class, () -> auth.login(RESIDENT, RESIDENT_PASSWORD));
        clock.advance(Duration.ofSeconds(1));
        assertEquals(RESIDENT, auth.login(RESIDENT, RESIDENT_PASSWORD).user().username());
    }

    /**
     * Unknown usernames are counted and blocked the same way, so the block does not reveal which usernames exist.
     */


    @Test
    void unknownUsernamesAreBlockedLikeRealOnes() {
        for (int i = 0; i < 5; i++)
        {
            assertThrows(UnauthorizedException.class, () -> auth.login("nobody", "wrong"));
        }
        assertThrows(TooManyRequestsException.class, () -> auth.login("nobody", "wrong"));
    }

    /**
     * A successful login clears the failure count, so four failures before and four after do not block.
     */


    @Test
    void successfulLoginResetsTheFailureCount() {
        failFor(RESIDENT, 4);
        assertEquals(RESIDENT, auth.login(RESIDENT, RESIDENT_PASSWORD).user().username());
        failFor(RESIDENT, 4);
        assertEquals(RESIDENT, auth.login(RESIDENT, RESIDENT_PASSWORD).user().username());
    }

    /**
     * Failures that fall in a new minute start a new count.
     */


    @Test
    void failuresInANewMinuteStartANewCount() {
        failFor(RESIDENT, 4);
        clock.advance(Duration.ofSeconds(61));
        failFor(RESIDENT, 4);
        assertEquals(RESIDENT, auth.login(RESIDENT, RESIDENT_PASSWORD).user().username());
    }

    /**
     * A token is accepted at exactly its expiry time and rejected one second later.
     */


    @Test
    void tokenIsValidAtExactlyTheTtlAndRejectedOneSecondLater() {
        AuthService.LoginResult result = auth.login(RESIDENT, RESIDENT_PASSWORD);
        assertEquals(START.plus(TTL), result.expiresAt());
        clock.advance(TTL);
        assertTrue(auth.userForToken(result.token()).isPresent());
        clock.advance(Duration.ofSeconds(1));
        assertFalse(auth.userForToken(result.token()).isPresent());
        assertFalse(auth.userForToken(result.token()).isPresent());
    }

    /**
     * Every token is different and is 32 random bytes encoded as 43 URL-safe characters.
     */


    @Test
    void tokensAreUniqueAndUrlSafe() {
        String first = auth.login(RESIDENT, RESIDENT_PASSWORD).token();
        String second = auth.login(RESIDENT, RESIDENT_PASSWORD).token();
        assertNotEquals(first, second);
        assertEquals(TOKEN_LENGTH, first.length());
        assertTrue(first.matches("[A-Za-z0-9_-]+"));
    }

    /**
     * Logging out ends the token straight away.
     */


    @Test
    void logoutEndsTheToken() {
        String token = auth.login(RESIDENT, RESIDENT_PASSWORD).token();
        auth.logout(token);
        assertFalse(auth.userForToken(token).isPresent());
    }

    /**
     * Logging out with a null, unknown or already expired token does nothing and does not fail.
     */


    @Test
    void logoutOfMissingOrUnknownTokenIsIgnored() {
        assertDoesNotThrow(() -> auth.logout(null));
        assertDoesNotThrow(() -> auth.logout("no-such-token"));
        assertFalse(auth.userForToken(null).isPresent());
    }

    /**
     * Many threads logging in and out at once each get a token that works until they log out.
     *
     * @throws Exception when a worker thread fails
     */


    @Test
    void concurrentLoginsAndLogoutsStayConsistent() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try
        {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < 40; i++)
            {
                results.add(pool.submit(() -> {
                    AuthService.LoginResult result = auth.login(RESIDENT, RESIDENT_PASSWORD);
                    boolean worksBeforeLogout = auth.userForToken(result.token()).isPresent();
                    auth.logout(result.token());
                    return worksBeforeLogout && auth.userForToken(result.token()).isEmpty();
                }));
            }
            for (Future<Boolean> result : results)
            {
                assertTrue(result.get());
            }
        }
        finally
        {
            pool.shutdownNow();
        }
    }

    /**
     * Makes a number of failed logins for one username, all in the same minute.
     *
     * @param username the username to fail for
     * @param count    how many wrong passwords to send
     */


    private void failFor
    (
        String username,
        int count
    ) {
        for (int i = 0; i < count; i++)
        {
            assertThrows(UnauthorizedException.class, () -> auth.login(username, "wrong"));
        }
    }
}