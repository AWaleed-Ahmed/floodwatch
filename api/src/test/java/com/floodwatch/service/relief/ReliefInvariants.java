/*
 * File:    ReliefInvariants.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Test helper that checks the stock rules after a scenario.
 */


package com.floodwatch.service.relief;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.floodwatch.model.Allocation;
import com.floodwatch.model.AllocationStatus;
import com.floodwatch.model.Supply;
import com.floodwatch.repository.AllocationRepository;
import com.floodwatch.repository.SupplyRepository;


/**
 * Checks the stock invariants for every supply: available and allocated are never negative, and allocated equals
 * the sum of the RESERVED quantities for that supply.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public final class ReliefInvariants {


    /**
     * Not meant to be created; the check is a static method.
     */


    private ReliefInvariants() {
    }

    /**
     * Fails the test when any supply breaks the stock invariants.
     *
     * @param supplies    the supply storage to check
     * @param allocations the allocation storage to check against
     */


    public static void assertHold
    (
        SupplyRepository supplies,
        AllocationRepository allocations
    ) {
        for (Supply supply : supplies.findAll())
        {
            assertTrue(supply.available() >= 0, "available must not be negative for " + supply.id());
            assertTrue(supply.allocated() >= 0, "allocated must not be negative for " + supply.id());
            int reserved = 0;
            for (Allocation allocation : allocations.findAll())
            {
                if (allocation.supplyId().equals(supply.id()) && allocation.status() == AllocationStatus.RESERVED)
                {
                    reserved += allocation.quantity();
                }
            }
            assertEquals(reserved, supply.allocated(), "allocated must equal reserved quantities for " + supply.id());
        }
    }
}