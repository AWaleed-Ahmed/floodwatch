/*
 * File:    RequiresRole.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Marks an endpoint that only logged-in users with certain roles may call.
 */


package com.floodwatch.security;

import com.floodwatch.model.Role;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Marks a controller method, or a whole controller class, as needing a login token. A method
 * annotation replaces the class annotation. Endpoints without this annotation stay public.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequiresRole {


    /**
     * The roles that may call the endpoint.
     *
     * @return the allowed roles; an empty list means any logged-in user
     */


    Role[] value();
}