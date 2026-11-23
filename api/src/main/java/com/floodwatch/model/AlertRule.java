/*
 * File:    AlertRule.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: The administrator's rule that decides when the scheduler raises alerts for an area.
 */


package com.floodwatch.model;


/**
 * Rule for one area, checked by the alert scheduler. The area id is the rule's id.
 *
 * @param areaId   id of the area the rule covers
 * @param minLevel lowest assessed risk level that raises an alert
 * @param enabled  false keeps the rule but stops the scheduler from using it
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record AlertRule
(
    String areaId,
    RiskLevel minLevel,
    boolean enabled
) {


}