/*
 * File:    LoginAttemptLimiter.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Blocks a username for the rest of a minute after five failed logins.
 */


package com.floodwatch.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;


/**
 * Counts failed logins per username. Five failures within one minute block that username until the
 * minute ends, even for the correct password. A successful login clears the count. Blocked attempts
 * are not counted again, so the block cannot be extended by retrying. Usernames are counted whether
 * or not the account exists, so the block does not reveal which usernames are real.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Component
public class LoginAttemptLimiter {


    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    /**
     * Creates the limiter.
     *
     * @param clock source of the current time
     */


    public LoginAttemptLimiter
    (
        Clock clock
    ) {
        this.clock = clock;
    }

    /**
     * Tells whether a username is blocked right now.
     *
     * @param username the normalised username (trimmed, lower case)
     * @return true when the username has reached the failure limit and the minute has not ended
     */


    public boolean isBlocked
    (
        String username
    ) {
        Attempts current = attempts.get(username);
        return current != null
                && current.failures() >= MAX_FAILURES
                && clock.instant().isBefore(current.windowEnd());
    }

    /**
     * Counts one failed login. A new minute starts when the previous one has ended.
     *
     * @param username the normalised username (trimmed, lower case)
     */


    public void recordFailure
    (
        String username
    ) {
        Instant now = clock.instant();
        attempts.values().removeIf(entry -> !now.isBefore(entry.windowEnd()));
        attempts.compute(username, (key, current) -> {
            if (current == null || !now.isBefore(current.windowEnd()))
            {
                return new Attempts(now, 1);
            }
            return new Attempts(current.windowStart(), current.failures() + 1);
        });
    }

    /**
     * Clears the failure count after a successful login.
     *
     * @param username the normalised username (trimmed, lower case)
     */


    public void reset
    (
        String username
    ) {
        attempts.remove(username);
    }


    /**
     * Failures counted in one minute, starting when the first failure happened.
     *
     * @param windowStart when the counted minute started
     * @param failures    failed logins counted in that minute
     * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
     */


    private record Attempts
    (
        Instant windowStart,
        int failures
    ) {


        /**
         * Returns the moment the counted minute ends.
         *
         * @return the window start plus one minute
         */


        Instant windowEnd() {
            return windowStart.plus(WINDOW);
        }
    }
}