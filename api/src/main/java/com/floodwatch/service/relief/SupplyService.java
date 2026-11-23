/*
 * File:    SupplyService.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Lists, adds, corrects and deletes relief supplies.
 */


package com.floodwatch.service.relief;

import com.floodwatch.model.Allocation;
import com.floodwatch.model.AllocationStatus;
import com.floodwatch.model.Supply;
import com.floodwatch.repository.AllocationRepository;
import com.floodwatch.repository.SupplyRepository;
import com.floodwatch.service.ConflictException;
import com.floodwatch.service.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Service;


/**
 * Manages the stock of relief items. Every change runs under the shared relief lock, so the stock seen by
 * an allocation is never half-updated.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Service
public class SupplyService {


    private static final String SUPPLY_ID_PREFIX = "sup-";
    private static final String NOT_FOUND = "Supply not found.";
    private static final String DUPLICATE = "A supply with this item name already exists.";
    private static final String STALE = "This supply was changed by someone else. Reload and try again.";
    private static final String IN_USE = "This supply has reserved allocations and cannot be deleted.";

    private final SupplyRepository supplies;
    private final AllocationRepository allocations;
    private final ReliefLock lock;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param supplies    supply storage
     * @param allocations allocation storage, used to refuse deleting a supply in use
     * @param lock        the shared relief lock
     * @param clock       source of the current time
     */


    public SupplyService
    (
        SupplyRepository supplies,
        AllocationRepository allocations,
        ReliefLock lock,
        Clock clock
    ) {
        this.supplies = supplies;
        this.allocations = allocations;
        this.lock = lock;
        this.clock = clock;
    }

    /**
     * Lists every supply.
     *
     * @return all supplies
     */


    public List<Supply> list() {
        return supplies.findAll();
    }

    /**
     * Adds a new supply with nothing allocated. The item name must be unique ignoring case and spaces.
     *
     * @param item      the item name
     * @param unit      the unit of measure
     * @param available the units in stock
     * @return the saved supply
     * @throws ConflictException when another supply has the same item name
     */


    public Supply create
    (
        String item, String unit,
        int available
    ) {
        ReentrantLock monitor = lock.monitor();
        monitor.lock();
        try
        {
            String key = comparable(item);
            if (supplies.findAll().stream().anyMatch(existing -> comparable(existing.item()).equals(key)))
            {
                throw new ConflictException(DUPLICATE);
            }
            Instant now = clock.instant();
            return supplies.save(new Supply(SUPPLY_ID_PREFIX + UUID.randomUUID(), item, unit, available, 0, 1L, now));
        }
        finally
        {
            monitor.unlock();
        }
    }

    /**
     * Corrects the stock of one supply. Allocated units are not touched.
     *
     * @param id        the supply id
     * @param available the corrected number of units in stock
     * @param version   the version the caller last read
     * @return the changed supply
     * @throws NotFoundException when the supply does not exist
     * @throws ConflictException when the version is stale
     */


    public Supply correctStock
    (
        String id,
        int available,
        long version
    ) {
        ReentrantLock monitor = lock.monitor();
        monitor.lock();
        try
        {
            Supply current = find(id);
            if (current.version() != version)
            {
                throw new ConflictException(STALE);
            }
            return supplies.save(current.withStock(available, current.allocated(), clock.instant()));
        }
        finally
        {
            monitor.unlock();
        }
    }

    /**
     * Deletes a supply. A supply with a RESERVED allocation cannot be deleted.
     *
     * @param id the supply id
     * @throws NotFoundException when the supply does not exist
     * @throws ConflictException when a RESERVED allocation uses the supply
     */


    public void delete
    (
        String id
    ) {
        ReentrantLock monitor = lock.monitor();
        monitor.lock();
        try
        {
            find(id);
            boolean reserved = allocations.findAll().stream()
                    .anyMatch(allocation -> allocation.supplyId().equals(id) && allocation.status() == AllocationStatus.RESERVED);
            if (reserved)
            {
                throw new ConflictException(IN_USE);
            }
            supplies.delete(id);
        }
        finally
        {
            monitor.unlock();
        }
    }

    /**
     * Finds one supply.
     *
     * @param id the supply id
     * @return the supply
     * @throws NotFoundException when the supply does not exist
     */


    private Supply find
    (
        String id
    ) {
        return supplies.findById(id).orElseThrow(() -> new NotFoundException(NOT_FOUND));
    }

    /**
     * Makes an item name comparable: lower case with every whitespace character removed.
     *
     * @param item the item name
     * @return the comparable form
     */


    private static String comparable
    (
        String item
    ) {
        return item.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }
}