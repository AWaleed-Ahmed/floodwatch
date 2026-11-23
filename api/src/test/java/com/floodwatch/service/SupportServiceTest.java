/*
 * File:    SupportServiceTest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Checks the support address rules and the URL encoding of the mailto link.
 */


package com.floodwatch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.floodwatch.config.AppProperties;
import com.floodwatch.model.SupportContact;
import org.junit.jupiter.api.Test;


/**
 * Unit tests for the support contact. No Spring context is needed.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


class SupportServiceTest {


    /**
     * The mailto link encodes the subject with %20, not +, and keeps the address as it is.
     */


    @Test
    void mailtoEncodesTheSubjectWithPercentTwenty() {
        SupportContact contact = serviceFor("support@example.com").contact();
        assertThat(contact.mailto()).isEqualTo("mailto:support@example.com?subject=FloodWatch%20support");
        assertThat(contact.subject()).isEqualTo("FloodWatch support");
        assertThat(contact.email()).isEqualTo("support@example.com");
    }

    /**
     * A blank address uses the built-in default, and surrounding spaces are removed.
     */


    @Test
    void blankAddressUsesTheDefaultAndSpacesAreTrimmed() {
        assertThat(serviceFor("   ").contact().email()).isEqualTo(AppProperties.Support.DEFAULT_EMAIL);
        assertThat(serviceFor("  support@example.com  ").contact().email()).isEqualTo("support@example.com");
    }

    /**
     * A malformed address fails when the settings are built, with a message that names the setting.
     */


    @Test
    void malformedAddressFailsFast() {
        assertThatThrownBy(() -> new AppProperties.Support("not-an-address"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SUPPORT_EMAIL");
        assertThatThrownBy(() -> new AppProperties.Support("a b@example.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Builds the service with a given address and no other settings.
     *
     * @param email the address to configure
     * @return the support service
     */


    private static SupportService serviceFor
    (
        String email
    ) {
        AppProperties props = new AppProperties(null, null, null, null, null, null, null, new AppProperties.Support(email));
        return new SupportService(props);
    }
}