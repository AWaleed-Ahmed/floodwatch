/*
 * File:    AllocationService.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Reserves supplies for assigned incidents, marks them delivered, and releases them when an incident resolves.
 */


package com.floodwatch.service.relief;

import com.floodwatch.model.Allocation;
import com.floodwatch.model.AllocationStatus;
import com.floodwatch.model.Incident;
import com.floodwatch.model.IncidentStatus;
import com.floodwatch.model.Supply;
import com.floodwatch.model.User;
import com.floodwatch.repository.AllocationRepository;
import com.floodwatch.repository.SupplyRepository;
import com.floodwatch.service.BusinessRuleException;
import com.floodwatch.service.ConflictException;
import com.floodwatch.service.NotFoundException;
import com.floodwatch.service.incidents.IncidentService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Service;


/**
 * Allocates supplies to incidents. Only ASSIGNED incidents can receive supplies. The stock check, the stock
 * update and the allocation save happen under the shared relief lock, so the stock never goes negative and the
 * invariant (allocated equals the sum of RESERVED quantities) holds after every operation.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Service
public class AllocationService {


    private static final String ALLOCATION_ID_PREFIX = "alc-";
    private static final String SUPPLY_NOT_FOUND = "Supply not found.";
    private static final String ALLOCATION_NOT_FOUND = "Allocation not found.";
    private static final String NOT_ASSIGNED = "Supplies can only go to an ASSIGNED incident.";
    private static final String NOT_RESERVED = "Only a reserved allocation can be delivered.";

    private final AllocationRepository allocations;
    private final SupplyRepository supplies;
    private final IncidentService incidents;
    private final ReliefLock lock;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param allocations allocation storage
     * @param supplies    supply storage
     * @param incidents   incident service, used to check the incident status
     * @param lock        the shared relief lock
     * @param clock       source of the current time
     */


    public AllocationService
    (
        AllocationRepository allocations,
        SupplyRepository supplies,
        IncidentService incidents,
        ReliefLock lock,
        Clock clock
    ) {
        this.allocations = allocations;
        this.supplies = supplies;
        this.incidents = incidents;
        this.lock = lock;
        this.clock = clock;
    }

    /**
     * Reserves units of a supply for an ASSIGNED incident. The units move from available to allocated.
     *
     * @param supplyId   the supply to reserve
     * @param incidentId the incident receiving the supplies
     * @param quantity   the number of units, from 1 up to the maximum quantity
     * @param actor      the coordinator or admin making the allocation
     * @return the new RESERVED allocation
     * @throws NotFoundException       when the supply or the incident does not exist
     * @throws BusinessRuleException   when the incident is not ASSIGNED, or fewer units are available than requested
     */


    public Allocation allocate
    (
        String supplyId, String incidentId,
        int quantity,
        User actor
    ) {
        ReentrantLock monitor = lock.monitor();
        monitor.lock();
        try
        {
            Supply supply = supplies.findById(supplyId).orElseThrow(() -> new NotFoundException(SUPPLY_NOT_FOUND));
            Incident incident = incidents.find(incidentId);
            if (incident.status() != IncidentStatus.ASSIGNED)
            {
                throw new BusinessRuleException(NOT_ASSIGNED);
            }
            if (quantity > supply.available())
            {
                throw new BusinessRuleException("Only " + supply.available() + " " + supply.unit()
                        + " of " + supply.item() + " left.");
            }
            Instant now = clock.instant();
            Allocation allocation = new Allocation(ALLOCATION_ID_PREFIX + UUID.randomUUID(), supplyId, incidentId,
                    quantity, AllocationStatus.RESERVED, actor.id(), now, null);
            allocations.save(allocation);
            supplies.save(supply.withStock(supply.available() - quantity, supply.allocated() + quantity, now));
            return allocation;
        }
        finally
        {
            monitor.unlock();
        }
    }

    /**
     * Marks a RESERVED allocation as delivered. The units leave the allocated count, because they are used up.
     *
     * @param allocationId the allocation to deliver
     * @return the delivered allocation
     * @throws NotFoundException when the allocation or its supply does not exist
     * @throws ConflictException when the allocation is not RESERVED
     */


    public Allocation deliver
    (
        String allocationId
    ) {
        ReentrantLock monitor = lock.monitor();
        monitor.lock();
        try
        {
            Allocation allocation = allocations.findById(allocationId)
                    .orElseThrow(() -> new NotFoundException(ALLOCATION_NOT_FOUND));
            if (allocation.status() != AllocationStatus.RESERVED)
            {
                throw new ConflictException(NOT_RESERVED);
            }
            Supply supply = supplies.findById(allocation.supplyId())
                    .orElseThrow(() -> new NotFoundException(SUPPLY_NOT_FOUND));
            Instant now = clock.instant();
            supplies.save(supply.withStock(supply.available(), supply.allocated() - allocation.quantity(), now));
            return allocations.save(allocation.closed(AllocationStatus.DELIVERED, now));
        }
        finally
        {
            monitor.unlock();
        }
    }

    /**
     * Releases every RESERVED allocation of a resolved incident: the units go back from allocated to available
     * and the allocation becomes RELEASED. DELIVERED allocations are left alone. Running it again changes nothing.
     *
     * @param incidentId the resolved incident
     * @throws IllegalStateException when a reserved allocation points at a supply that no longer exists
     */


    public void releaseFor
    (
        String incidentId
    ) {
        ReentrantLock monitor = lock.monitor();
        monitor.lock();
        try
        {
            Instant now = clock.instant();
            for (Allocation allocation : allocations.findAll())
            {
                if (allocation.incidentId().equals(incidentId) && allocation.status() == AllocationStatus.RESERVED)
                {
                    Supply supply = supplies.findById(allocation.supplyId())
                            .orElseThrow(() -> new IllegalStateException("Reserved allocation " + allocation.id() + " has no supply."));
                    supplies.save(supply.withStock(supply.available() + allocation.quantity(),
                            supply.allocated() - allocation.quantity(), now));
                    allocations.save(allocation.closed(AllocationStatus.RELEASED, now));
                }
            }
        }
        finally
        {
            monitor.unlock();
        }
    }

    /**
     * Lists allocations in the order they were made, optionally only those for one incident.
     *
     * @param incidentId the incident to keep, or null for every allocation
     * @return the matching allocations
     */


    public List<Allocation> list
    (
        String incidentId
    ) {
        return allocations.findAll().stream()
                .filter(allocation -> incidentId == null || allocation.incidentId().equals(incidentId))
                .toList();
    }
}