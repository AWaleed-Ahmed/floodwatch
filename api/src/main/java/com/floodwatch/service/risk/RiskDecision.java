/*
 * File:    RiskDecision.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: The level and plain-language reason that a risk rule decides on.
 */


package com.floodwatch.service.risk;

import com.floodwatch.model.RiskLevel;


/**
 * A level decided by one risk rule, with the reason shown to the user.
 *
 * @param level  the decided risk level
 * @param reason plain-language explanation of the decision
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record RiskDecision
(
    RiskLevel level,
    String reason
) {


}