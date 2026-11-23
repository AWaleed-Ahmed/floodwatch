/*
 * File:    SupplyController.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: REST endpoints for relief supplies and their stock.
 */


package com.floodwatch.controller;

import com.floodwatch.model.Role;
import com.floodwatch.model.Supply;
import com.floodwatch.security.RequiresRole;
import com.floodwatch.service.relief.SupplyService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


/**
 * Feature 6: relief supplies (UC-06). Responders can see stock; coordinators and admins manage it; only admins
 * can delete a supply.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@RestController
@RequestMapping("/api/supplies")
public class SupplyController {


    private final SupplyService supplies;

    /**
     * Creates the controller.
     *
     * @param supplies the supply service
     */


    public SupplyController
    (
        SupplyService supplies
    ) {
        this.supplies = supplies;
    }

    /**
     * Lists every supply with its stock.
     *
     * @return all supplies
     */


    @GetMapping
    @RequiresRole({Role.RESPONDER, Role.COORDINATOR, Role.ADMIN})
    public List<Supply> list() {
        return supplies.list();
    }

    /**
     * Adds a supply to the stock list.
     *
     * @param body the item name, unit and units in stock
     * @return the new supply
     */


    @PostMapping
    @RequiresRole({Role.COORDINATOR, Role.ADMIN})
    @ResponseStatus(HttpStatus.CREATED)
    public Supply create
    (
        @Valid @RequestBody CreateSupplyRequest body
    ) {
        return supplies.create(body.item(), body.unit(), WholeNumbers.of("available", body.available()));
    }

    /**
     * Corrects the stock of one supply, guarded by the version the caller last read.
     *
     * @param id   the supply id
     * @param body the corrected stock and the version read
     * @return the changed supply
     */


    @PatchMapping("/{id}")
    @RequiresRole({Role.COORDINATOR, Role.ADMIN})
    public Supply correct
    (
        @PathVariable String id,
        @Valid @RequestBody SupplyStockRequest body
    ) {
        return supplies.correctStock(id, WholeNumbers.of("available", body.available()), body.version());
    }

    /**
     * Deletes a supply that has no reserved allocations.
     *
     * @param id the supply id
     */


    @DeleteMapping("/{id}")
    @RequiresRole({Role.ADMIN})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete
    (
        @PathVariable String id
    ) {
        supplies.delete(id);
    }
}