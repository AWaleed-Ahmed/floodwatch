/*
 * File:    AlertRuleController.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: REST endpoints that let administrators read and change the alert rule of each area.
 */


package com.floodwatch.controller;

import com.floodwatch.model.AlertRule;
import com.floodwatch.model.Role;
import com.floodwatch.security.RequiresRole;
import com.floodwatch.service.alerts.AlertRuleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * Feature 3 alert rules, for administrators only.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@RestController
@RequestMapping("/api/admin/alert-rules")
@RequiresRole({Role.ADMIN})
public class AlertRuleController {


    private final AlertRuleService rules;

    /**
     * Creates the controller.
     *
     * @param rules the alert rule service
     */


    public AlertRuleController
    (
        AlertRuleService rules
    ) {
        this.rules = rules;
    }

    /**
     * Lists every alert rule.
     *
     * @return all rules, ordered by area id
     */


    @GetMapping
    public List<AlertRule> list() {
        return rules.list();
    }

    /**
     * Creates or replaces the rule of one area.
     *
     * @param areaId id of the area
     * @param body   the minimum level and whether the rule is enabled
     * @return the saved rule
     */


    @PutMapping("/{areaId}")
    public AlertRule update
    (
        @PathVariable String areaId,
        @Valid @RequestBody AlertRuleRequest body
    ) {
        return rules.update(areaId, body.minLevel(), body.enabled());
    }
}