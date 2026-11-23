/*
 * File:    CurrentUser.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Lets controllers read the user who logged in for the current request.
 */


package com.floodwatch.security;

import com.floodwatch.model.User;
import com.floodwatch.service.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;


/**
 * Stores and reads the logged-in user of one request. The interceptor stores the user before an
 * endpoint marked with RequiresRole runs, so controllers can call of(request) safely.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public final class CurrentUser {


    private static final String ATTRIBUTE = CurrentUser.class.getName() + ".user";

    /**
     * Not meant to be created; use the static methods.
     */


    private CurrentUser() {
    }

    /**
     * Stores the logged-in user on the request.
     *
     * @param request the request being handled
     * @param user    the user whose token was accepted
     */


    static void store
    (
        HttpServletRequest request,
        User user
    ) {
        request.setAttribute(ATTRIBUTE, user);
    }

    /**
     * Returns the logged-in user of the request.
     *
     * @param request the request being handled
     * @return the logged-in user
     * @throws UnauthorizedException when no user was stored, which means the endpoint is not protected
     */


    public static User of
    (
        HttpServletRequest request
    ) {
        if (request.getAttribute(ATTRIBUTE) instanceof User user)
        {
            return user;
        }
        throw new UnauthorizedException("Log in to continue.");
    }
}