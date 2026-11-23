/*
 * File:    AuthController.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: REST endpoints for logging in, logging out and reading the logged-in user.
 */


package com.floodwatch.controller;

import com.floodwatch.model.User;
import com.floodwatch.security.BearerToken;
import com.floodwatch.security.CurrentUser;
import com.floodwatch.security.RequiresRole;
import com.floodwatch.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


/**
 * Login, logout and the current user. Login and logout are public; /me needs any logged-in user.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@RestController
@RequestMapping("/api/auth")
public class AuthController {


    private final AuthService auth;

    /**
     * Creates the controller.
     *
     * @param auth the login service
     */


    public AuthController
    (
        AuthService auth
    ) {
        this.auth = auth;
    }

    /**
     * Checks the username and password and returns a login token.
     *
     * @param body the username and password
     * @return the token, when it expires and the logged-in user's details
     */


    @PostMapping("/login")
    public LoginResponse login
    (
        @Valid @RequestBody LoginRequest body
    ) {
        AuthService.LoginResult result = auth.login(body.username(), body.password());
        User user = result.user();
        return new LoginResponse(result.token(), result.expiresAt(), user.username(), user.displayName(), user.role());
    }

    /**
     * Ends the caller's login. Always answers 204, even when the token is missing or already invalid.
     *
     * @param request the request whose Authorization header holds the token
     */


    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout
    (
        HttpServletRequest request
    ) {
        BearerToken.parse(request.getHeader(HttpHeaders.AUTHORIZATION)).ifPresent(auth::logout);
    }

    /**
     * Returns the account of the caller. Any logged-in user may call this.
     *
     * @param request the request carrying the login token
     * @return the caller's details, without the password hash
     */


    @GetMapping("/me")
    @RequiresRole({})
    public MeResponse me
    (
        HttpServletRequest request
    ) {
        User user = CurrentUser.of(request);
        return new MeResponse(user.id(), user.username(), user.displayName(), user.role(), user.teamId());
    }
}