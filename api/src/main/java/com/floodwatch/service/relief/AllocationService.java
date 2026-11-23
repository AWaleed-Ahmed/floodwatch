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
import com.floodwatch.service.ServiceUnavailableException;
import com.floodwatch.service.incidents.IncidentService;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


/**
 * Allocates supplies to incidents. Only ASSIGNED incidents can receive supplies.
 * <p>
 * Each operation changes two files: allocations and supplies. Storage has no transactions, so when the second
 * write fails the first one is undone with a compensating write. The stock check, the writes and the undo all
 * run under the shared relief lock. Release writes the allocation as RELEASED before it returns the stock, and
 * undoes that if the stock write fails, so a retry can never return the same stock twice.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Service
public class AllocationService {


    private static final Logger LOG = LoggerFactory.getLogger(AllocationService.class);
    private static final String ALLOCATION_ID_PREFIX = "alc-";
    private static final String SUPPLY_NOT_FOUND = "Supply not found.";
    private static final String ALLOCATION_NOT_FOUND = "Allocation not found.";
    private static final String NOT_ASSIGNED = "Supplies can only go to an ASSIGNED incident.";
    private static final String NOT_RESERVED = "Only a reserved allocation can be delivered.";
    private static final String SAVE_FAILED = "Saving relief data failed; nothing was changed. Try again.";

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
     * If the stock write fails, the new allocation is deleted again.
     *
     * @param supplyId   the supply to reserve
     * @param incidentId the incident receiving the supplies
     * @param quantity   the number of units, from 1 up to the maximum quantity
     * @param actor      the coordinator or admin making the allocation
     * @return the new RESERVED allocation
     * @throws NotFoundException       when the supply or the incident does not exist
     * @throws BusinessRuleException   when the incident is not ASSIGNED, or fewer units are available than requested
     * @throws ServiceUnavailableException when the stock write fails; nothing is left changed
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
            try
            {
                supplies.save(supply.withStock(supply.available() - quantity, supply.allocated() + quantity, now));
            }
            catch (RuntimeException e)
            {
                LOG.warn("Saving stock after reserving allocation {} failed", allocation.id(), e);
                undo("allocation " + allocation.id(), () -> allocations.delete(allocation.id()));
                throw new ServiceUnavailableException(SAVE_FAILED);
            }
            return allocation;
        }
        finally
        {
            monitor.unlock();
        }
    }

    /**
     * Marks a RESERVED allocation as delivered. The units leave the allocated count, because they are used up.
     * The stock is written first; if the allocation write then fails, the stock is restored.
     *
     * @param allocationId the allocation to deliver
     * @return the delivered allocation
     * @throws NotFoundException when the allocation or its supply does not exist
     * @throws ConflictException when the allocation is not RESERVED
     * @throws ServiceUnavailableException when the allocation write fails; both records are left as they were
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
            Allocation delivered = allocation.closed(AllocationStatus.DELIVERED, now);
            try
            {
                allocations.save(delivered);
            }
            catch (RuntimeException e)
            {
                LOG.warn("Saving delivery of allocation {} failed", allocation.id(), e);
                undo("supply " + supply.id(), () -> supplies.save(supply));
                throw new ServiceUnavailableException(SAVE_FAILED);
            }
            return delivered;
        }
        finally
        {
            monitor.unlock();
        }
    }

    /**
     * Releases every RESERVED allocation of an incident. For each one, the allocation is written as RELEASED
     * first and the units go back from allocated to available second. If the second write fails, the allocation
     * is written back as RESERVED, so the next run returns the stock exactly once. DELIVERED allocations are left
     * alone, and running this again changes nothing.
     *
     * @param incidentId the resolved incident
     * @throws IllegalStateException when a reserved allocation points at a supply that no longer exists
     * @throws ServiceUnavailableException when a write fails; the allocation in progress is left RESERVED
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
                    allocations.save(allocation.closed(AllocationStatus.RELEASED, now));
                    try
                    {
                        supplies.save(supply.withStock(supply.available() + allocation.quantity(),
                                supply.allocated() - allocation.quantity(), now));
                    }
                    catch (RuntimeException e)
                    {
                        LOG.warn("Returning stock for allocation {} failed", allocation.id(), e);
                        undo("allocation " + allocation.id(), () -> allocations.save(allocation));
                        throw new ServiceUnavailableException(SAVE_FAILED);
                    }
                }
            }
        }
        finally
        {
            monitor.unlock();
        }
    }

    /**
     * Finds every RESERVED allocation whose incident is RESOLVED and releases it. This repairs reservations that
     * a failed release left behind. Each incident is handled separately, and failures are logged, not thrown.
     * Running it again changes nothing once the reservations are released.
     */


    public void releaseStuckReservations() {
        try
        {
            Set<String> incidentIds = new LinkedHashSet<>();
            for (Allocation allocation : allocations.findAll())
            {
                if (allocation.status() == AllocationStatus.RESERVED)
                {
                    incidentIds.add(allocation.incidentId());
                }
            }
            for (String incidentId : incidentIds)
            {
                releaseIfResolved(incidentId);
            }
        }
        catch (RuntimeException e)
        {
            LOG.error("Checking for stuck relief reservations failed", e);
        }
    }

    /**
     * Releases one incident's reservations if the incident is RESOLVED. Failures are logged, not thrown.
     *
     * @param incidentId the incident to check
     */


    private void releaseIfResolved
    (
        String incidentId
    ) {
        try
        {
            if (incidents.find(incidentId).status() == IncidentStatus.RESOLVED)
            {
                releaseFor(incidentId);
            }
        }
        catch (RuntimeException e)
        {
            LOG.error("Could not release reserved supplies for incident {}", incidentId, e);
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

    /**
     * Runs an undo step after a later write failed. If the undo fails too, the error is logged with the record
     * it was meant to restore, so the data can be repaired by hand.
     *
     * @param description what the undo restores, used in the log message
     * @param undo        the write that puts the earlier record back
     */


    private static void undo
    (
        String description,
        Runnable undo
    ) {
        try
        {
            undo.run();
        }
        catch (RuntimeException e)
        {
            LOG.error("Undo failed for {}; this record may need repair", description, e);
        }
    }
}