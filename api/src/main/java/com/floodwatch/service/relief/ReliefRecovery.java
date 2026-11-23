/*
 * File:    ReliefRecovery.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Repairs stuck relief reservations once at startup and then on a fixed schedule.
 */


package com.floodwatch.service.relief;

import java.util.concurrent.TimeUnit;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


/**
 * Runs AllocationService.releaseStuckReservations when the application starts and every 10 minutes after that.
 * The work is idempotent, so running it often is harmless.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Component
public class ReliefRecovery {


    private static final long RECOVERY_INTERVAL_MINUTES = 10;

    private final AllocationService allocations;

    /**
     * Creates the recovery job.
     *
     * @param allocations the allocation service that performs the repair
     */


    public ReliefRecovery
    (
        AllocationService allocations
    ) {
        this.allocations = allocations;
    }

    /**
     * Repairs stuck reservations once the application is ready.
     */


    @EventListener(ApplicationReadyEvent.class)
    public void recoverAtStartup() {
        allocations.releaseStuckReservations();
    }

    /**
     * Repairs stuck reservations on the fixed schedule.
     */


    @Scheduled(fixedDelay = RECOVERY_INTERVAL_MINUTES, timeUnit = TimeUnit.MINUTES)
    public void recoverPeriodically() {
        allocations.releaseStuckReservations();
    }
}