/*
 * File:    ReliefLock.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: The one lock that guards every change to stock and allocations.
 */


package com.floodwatch.service.relief;

import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Component;


/**
 * Holds the lock shared by the supply, allocation and release operations. Stock checks, stock updates and
 * allocation saves all run while holding it, so two coordinators cannot both take the last units.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Component
public class ReliefLock {


    private final ReentrantLock monitor = new ReentrantLock();

    /**
     * Returns the shared lock. Callers must lock and unlock it in a try and finally block.
     *
     * @return the shared lock
     */


    public ReentrantLock monitor() {
        return monitor;
    }
}