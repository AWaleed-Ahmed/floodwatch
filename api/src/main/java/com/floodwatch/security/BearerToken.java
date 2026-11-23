/*
 * File:    BearerToken.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Reads the login token out of an Authorization header.
 */


package com.floodwatch.security;

import java.util.Optional;


/**
 * Reads "Bearer &lt;token&gt;" headers. The scheme name is not case-sensitive, as HTTP allows. The token
 * must follow exactly one space and must contain no spaces, so extra spaces are rejected.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public final class BearerToken {


    private static final String PREFIX = "Bearer ";

    /**
     * Not meant to be created; use the static methods.
     */


    private BearerToken() {
    }

    /**
     * Returns the token from an Authorization header.
     *
     * @param header the header value, may be null
     * @return the token, or empty when the header is missing, is not a Bearer header or has no valid token
     */


    public static Optional<String> parse
    (
        String header
    ) {
        if (header == null || !header.regionMatches(true, 0, PREFIX, 0, PREFIX.length()))
        {
            return Optional.empty();
        }
        String token = header.substring(PREFIX.length());
        if (token.isEmpty() || token.chars().anyMatch(Character::isWhitespace))
        {
            return Optional.empty();
        }
        return Optional.of(token);
    }
}