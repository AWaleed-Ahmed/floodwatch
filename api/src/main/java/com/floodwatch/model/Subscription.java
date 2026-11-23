/*
 * File:    Subscription.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: A resident's request to receive alerts for one area at or above a risk level.
 */


package com.floodwatch.model;

import java.time.Instant;


/**
 * Feature 3: one user follows one area. Alerts for that area reach the user when their level is at least minLevel.
 *
 * @param id        unique subscription id
 * @param userId    id of the user who follows the area
 * @param areaId    id of the followed area
 * @param minLevel  lowest risk level that triggers an alert for this subscription
 * @param createdAt when the subscription was first created
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record Subscription
(
    String id, String userId, String areaId,
    RiskLevel minLevel,
    Instant createdAt
) {


}