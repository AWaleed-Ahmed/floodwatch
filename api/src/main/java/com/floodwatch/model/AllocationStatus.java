/*
 * File:    AllocationStatus.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Life cycle states of a supply allocation to an incident.
 */


package com.floodwatch.model;


/**
 * An allocation starts RESERVED. It becomes DELIVERED when the supplies are handed over, or RELEASED when
 * the incident is resolved before delivery. DELIVERED and RELEASED are final.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public enum AllocationStatus {


    RESERVED, DELIVERED, RELEASED
}