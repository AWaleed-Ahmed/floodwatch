/*
 * File:    AlertRuleService.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Lets an administrator read and change the alert rule of each area.
 */


package com.floodwatch.service.alerts;

import com.floodwatch.model.AlertRule;
import com.floodwatch.model.RiskLevel;
import com.floodwatch.repository.AlertRuleRepository;
import com.floodwatch.repository.AreaRepository;
import com.floodwatch.service.NotFoundException;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;


/**
 * Feature 3 alert rules for administrators. Each known area has at most one rule.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Service
public class AlertRuleService {


    private final AlertRuleRepository rules;
    private final AreaRepository areas;

    /**
     * Creates the service.
     *
     * @param rules alert rule storage
     * @param areas area storage, used to reject unknown areas
     */


    public AlertRuleService
    (
        AlertRuleRepository rules,
        AreaRepository areas
    ) {
        this.rules = rules;
        this.areas = areas;
    }

    /**
     * Lists every alert rule, ordered by area id.
     *
     * @return all rules
     */


    public List<AlertRule> list() {
        return rules.findAll().stream()
                .sorted(Comparator.comparing(AlertRule::areaId))
                .toList();
    }

    /**
     * Creates or replaces the rule of one area.
     *
     * @param areaId   id of the area
     * @param minLevel lowest assessed level that raises an alert
     * @param enabled  whether the scheduler uses this rule
     * @return the saved rule
     * @throws NotFoundException when the area does not exist
     */


    public AlertRule update
    (
        String areaId,
        RiskLevel minLevel,
        boolean enabled
    ) {
        if (areas.findById(areaId).isEmpty())
        {
            throw new NotFoundException("Unknown area: " + areaId);
        }
        return rules.save(new AlertRule(areaId, minLevel, enabled));
    }
}