/*
 * File:    AuthControllerTest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: HTTP-level tests for login, logout, /me and the role rules, run against the real application.
 */


package com.floodwatch.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;


/**
 * Calls the real login, logout, /me and protected endpoints over HTTP with the test accounts.
 * Each failed-login test uses its own account, because the failure count is shared by the whole run.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {


    private static final String LOGIN = "/api/auth/login";
    private static final String LOGOUT = "/api/auth/logout";
    private static final String ME = "/api/auth/me";
    private static final String TEAM = "/api/test-only/team";
    private static final String ANYONE = "/api/test-only/anyone";
    private static final String INVALID = "Invalid username or password.";

    @Autowired
    private MockMvc mvc;

    /**
     * A correct login returns a token, when it expires and the account's public details, but no hash.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void loginReturnsTokenAndPublicDetails() throws Exception {
        mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(credentials("resident1", "resident1-demo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(jsonPath("$.username").value("resident1"))
                .andExpect(jsonPath("$.displayName").value("Resident One"))
                .andExpect(jsonPath("$.role").value("RESIDENT"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    /**
     * Username spacing and letter case are ignored at login.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void loginAcceptsPaddedUsernameInAnyCase() throws Exception {
        mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(credentials(" RESPONDER1 ", "responder1-demo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("responder1"))
                .andExpect(jsonPath("$.role").value("RESPONDER"));
    }

    /**
     * Blank or missing fields are refused with 400 before any password check.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void blankOrMissingFieldsReturn400() throws Exception {
        String[] bodies = {
            credentials("   ", "resident1-demo"),
            credentials("resident1", ""),
            "{\"username\":\"resident1\"}",
            "{}"
        };
        for (String body : bodies)
        {
            mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    /**
     * A body that is not JSON is refused with 400 in the standard error format.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void notJsonBodyReturns400() throws Exception {
        mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content("not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    /**
     * A password longer than 72 bytes is refused with 400, because BCrypt ignores the rest.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void passwordOver72BytesReturns400() throws Exception {
        mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(credentials("resident1", "x".repeat(73))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("72 bytes")));
    }

    /**
     * A wrong password and an unknown username get the same 401 response, so usernames cannot be guessed.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void wrongPasswordAndUnknownUserGetTheSame401() throws Exception {
        mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(credentials("resident1", "wrong")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value(INVALID));
        mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(credentials("nobody", "wrong")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value(INVALID));
    }

    /**
     * Five failed logins block the username with 429, and the correct password is refused too.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void fiveFailedLoginsReturn429EvenWithTheRightPassword() throws Exception {
        for (int i = 0; i < 5; i++)
        {
            mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(credentials("resident2", "wrong")))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(credentials("resident2", "resident2-demo")))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));
    }

    /**
     * /me returns the caller without the password hash.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void meReturnsTheCallerWithoutPasswordHash() throws Exception {
        String token = tokenFor("resident1", "resident1-demo");
        mvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("resident1"))
                .andExpect(jsonPath("$.role").value("RESIDENT"))
                .andExpect(jsonPath("$.teamId").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    /**
     * /me without a token is refused with 401.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void meWithoutTokenReturns401() throws Exception {
        mvc.perform(get(ME))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    /**
     * Malformed, unknown and empty Authorization headers all get 401, never 500.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void malformedAuthorizationHeadersReturn401() throws Exception {
        String token = tokenFor("resident1", "resident1-demo");
        String[] headers = {
            "Bearer",
            "Bearer ",
            "Bearer  " + token,
            "Bearer " + token + " ",
            "Basic xyz",
            "",
            "Bearer no-such-token"
        };
        for (String header : headers)
        {
            mvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, header))
                    .andExpect(status().isUnauthorized());
        }
    }

    /**
     * A logged-out token stops working, and logging out again still answers 204.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void logoutEndsTheTokenAndRepeatedLogoutIs204() throws Exception {
        String token = tokenFor("resident1", "resident1-demo");
        mvc.perform(post(LOGOUT).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(LOGOUT).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    /**
     * Logging out without a header or with a garbage header still answers 204.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void logoutWithoutValidTokenIsStill204() throws Exception {
        mvc.perform(post(LOGOUT)).andExpect(status().isNoContent());
        mvc.perform(post(LOGOUT).header(HttpHeaders.AUTHORIZATION, "Basic xyz")).andExpect(status().isNoContent());
    }

    /**
     * A responder-only endpoint refuses a resident with 403, accepts a responder, and refuses no token with 401.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void roleRuleReturns403ForWrongRoleAnd401ForNoToken() throws Exception {
        String resident = tokenFor("resident1", "resident1-demo");
        String responder = tokenFor("responder1", "responder1-demo");
        mvc.perform(get(TEAM).header(HttpHeaders.AUTHORIZATION, "Bearer " + resident))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mvc.perform(get(TEAM).header(HttpHeaders.AUTHORIZATION, "Bearer " + responder))
                .andExpect(status().isOk());
        mvc.perform(get(TEAM))
                .andExpect(status().isUnauthorized());
    }

    /**
     * A method-level rule replaces the class-level rule, so any logged-in user may call the endpoint.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void methodRuleOverridesClassRule() throws Exception {
        String resident = tokenFor("resident1", "resident1-demo");
        mvc.perform(get(ANYONE).header(HttpHeaders.AUTHORIZATION, "Bearer " + resident))
                .andExpect(status().isOk())
                .andExpect(content().string("resident1"));
        mvc.perform(get(ANYONE))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Builds a JSON login body.
     *
     * @param username the username to send
     * @param password the password to send
     * @return the JSON body text
     */


    private String credentials
    (
        String username, String password
    ) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    /**
     * Logs in and returns the token from the response.
     *
     * @param username the username to log in with
     * @param password the password to log in with
     * @return the Bearer token value
     * @throws Exception when the request cannot be performed
     */


    private String tokenFor
    (
        String username, String password
    ) throws Exception {
        MvcResult result = mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(credentials(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }
}