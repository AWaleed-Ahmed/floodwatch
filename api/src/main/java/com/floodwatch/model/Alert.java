/*
 * File:    Alert.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: One educational flood alert raised for an area, stored in the alert inbox.
 */


package com.floodwatch.model;

import java.time.Instant;


/**
 * An educational alert about an area. Demo alerts are created by administrators to test delivery and
 * never count toward the cooldown.
 *
 * @param id        unique alert id
 * @param areaId    id of the area the alert is about
 * @param level     risk level that triggered the alert
 * @param message   text shown to residents
 * @param reason    plain-language reason taken from the risk assessment
 * @param createdAt when the alert was raised
 * @param demo      true when an administrator created the alert for testing
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record Alert
(
    String id, String areaId,
    RiskLevel level,
    String message, String reason,
    Instant createdAt,
    boolean demo
) {


}