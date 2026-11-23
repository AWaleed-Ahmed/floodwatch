/*
 * File:    SupportController.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Public REST endpoint that returns the support contact.
 */


package com.floodwatch.controller;

import com.floodwatch.model.SupportContact;
import com.floodwatch.service.SupportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * Support contact. Public: no login needed.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@RestController
@RequestMapping("/api/support")
public class SupportController {


    private final SupportService support;

    /**
     * Creates the controller.
     *
     * @param support the support contact service
     */


    public SupportController
    (
        SupportService support
    ) {
        this.support = support;
    }

    /**
     * Returns the support address and a mailto link.
     *
     * @return the support contact
     */


    @GetMapping
    public SupportContact contact() {
        return support.contact();
    }
}