/*
 * File:    SupportService.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Builds the public support contact and its URL-encoded mailto link.
 */


package com.floodwatch.service;

import com.floodwatch.config.AppProperties;
import com.floodwatch.model.SupportContact;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;


/**
 * Returns the configured support address with a mailto link. The subject is encoded with %20 for spaces,
 * not +, because mail clients read %20 reliably.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Service
public class SupportService {


    static final String SUBJECT = "FloodWatch support";

    private final AppProperties props;

    /**
     * Creates the service.
     *
     * @param props application settings holding the support address
     */


    public SupportService
    (
        AppProperties props
    ) {
        this.props = props;
    }

    /**
     * Returns the support contact.
     *
     * @return the address, the subject and the mailto link
     */


    public SupportContact contact() {
        String email = props.support().email();
        String subject = URLEncoder.encode(SUBJECT, StandardCharsets.UTF_8).replace("+", "%20");
        return new SupportContact(email, SUBJECT, "mailto:" + email + "?subject=" + subject);
    }
}