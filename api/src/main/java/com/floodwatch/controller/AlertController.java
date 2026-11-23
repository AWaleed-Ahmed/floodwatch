/*
 * File:    AlertController.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: REST endpoints for a resident's alert list and the administrator's demo alert.
 */


package com.floodwatch.controller;

import com.floodwatch.model.Alert;
import com.floodwatch.model.Role;
import com.floodwatch.model.User;
import com.floodwatch.security.CurrentUser;
import com.floodwatch.security.RequiresRole;
import com.floodwatch.service.alerts.AlertQueryService;
import com.floodwatch.service.alerts.DemoAlertService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * Feature 3 alerts: the resident's alert list and the administrator's demo alert.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@RestController
@RequestMapping("/api")
public class AlertController {


    private final AlertQueryService queries;
    private final DemoAlertService demo;

    /**
     * Creates the controller.
     *
     * @param queries the resident alert list service
     * @param demo    the demo alert service
     */


    public AlertController
    (
        AlertQueryService queries,
        DemoAlertService demo
    ) {
        this.queries = queries;
        this.demo = demo;
    }

    /**
     * Lists alerts for the caller's subscribed areas, newest first, at most 50. Without since, the last 7 days.
     *
     * @param since ISO instant; only alerts created after it are returned
     * @param request the request carrying the login token
     * @return matching alerts, possibly empty
     */


    @GetMapping("/alerts/mine")
    @RequiresRole({Role.RESIDENT, Role.ADMIN})
    public List<Alert> mine
    (
        @RequestParam(required = false) String since,
        HttpServletRequest request
    ) {
        User user = CurrentUser.of(request);
        return queries.mine(user, since);
    }

    /**
     * Creates and delivers a demo alert. Only administrators may send one.
     *
     * @param body the area and optional level
     * @return HTTP 201 with the demo alert
     */


    @PostMapping("/admin/alerts/demo")
    @RequiresRole({Role.ADMIN})
    public ResponseEntity<Alert> sendDemo
    (
        @Valid @RequestBody DemoAlertRequest body
    ) {
        Alert alert = demo.raiseDemo(body.areaId(), body.level());
        return ResponseEntity.status(HttpStatus.CREATED).body(alert);
    }
}