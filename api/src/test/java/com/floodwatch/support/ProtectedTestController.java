/*
 * File:    ProtectedTestController.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Test-only endpoints that prove the RequiresRole rules work on a class and on a method.
 */


package com.floodwatch.support;

import com.floodwatch.model.Role;
import com.floodwatch.security.CurrentUser;
import com.floodwatch.security.RequiresRole;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * Endpoints that exist only in the test classpath. The class is limited to responders and admins;
 * the "anyone" method overrides that and lets any logged-in user in.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@RestController
@RequestMapping("/api/test-only")
@RequiresRole({Role.RESPONDER, Role.ADMIN})
public class ProtectedTestController {


    /**
     * Answers for responders and admins only, through the class-level rule.
     *
     * @return a fixed text
     */


    @GetMapping("/team")
    public String team() {
        return "team";
    }

    /**
     * Answers for any logged-in user, because the method rule replaces the class rule.
     *
     * @param request the request carrying the login token
     * @return the username of the caller
     */


    @GetMapping("/anyone")
    @RequiresRole({})
    public String anyone
    (
        HttpServletRequest request
    ) {
        return CurrentUser.of(request).username();
    }
}