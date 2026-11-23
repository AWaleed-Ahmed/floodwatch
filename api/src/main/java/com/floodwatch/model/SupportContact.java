/*
 * File:    SupportContact.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: The public support contact returned by GET /api/support.
 */


package com.floodwatch.model;


/**
 * Support contact that the app shows, with a ready-made mailto link.
 *
 * @param email   address residents write to
 * @param subject subject line used in the mailto link
 * @param mailto  mailto link with the subject URL-encoded
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record SupportContact
(
    String email, String subject, String mailto
) {


}