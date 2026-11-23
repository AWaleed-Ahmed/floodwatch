/*
 * File:    SubscriptionController.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: REST endpoints for following and unfollowing areas for alerts.
 */


package com.floodwatch.controller;

import com.floodwatch.model.Role;
import com.floodwatch.model.Subscription;
import com.floodwatch.model.User;
import com.floodwatch.security.CurrentUser;
import com.floodwatch.security.RequiresRole;
import com.floodwatch.service.alerts.SubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


/**
 * Feature 3 subscriptions. Residents and administrators may follow areas; each caller sees only their own.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@RestController
@RequestMapping("/api/subscriptions")
@RequiresRole({Role.RESIDENT, Role.ADMIN})
public class SubscriptionController {


    private final SubscriptionService subscriptions;

    /**
     * Creates the controller.
     *
     * @param subscriptions the subscription service
     */


    public SubscriptionController
    (
        SubscriptionService subscriptions
    ) {
        this.subscriptions = subscriptions;
    }

    /**
     * Follows an area. Answers 201 when a new subscription is created and 200 when the area was already followed.
     *
     * @param body    the area and the optional minimum level
     * @param request the request carrying the login token
     * @return the subscription
     */


    @PostMapping
    public ResponseEntity<Subscription> subscribe
    (
        @Valid @RequestBody SubscriptionRequest body,
        HttpServletRequest request
    ) {
        User user = CurrentUser.of(request);
        SubscriptionService.Result result = subscriptions.subscribe(user, body.areaId(), body.minLevel());
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.subscription());
    }

    /**
     * Lists the caller's subscriptions.
     *
     * @param request the request carrying the login token
     * @return the caller's subscriptions, possibly empty
     */


    @GetMapping("/mine")
    public List<Subscription> mine
    (
        HttpServletRequest request
    ) {
        return subscriptions.mine(CurrentUser.of(request));
    }

    /**
     * Stops following an area. Answers 404 for an unknown id or someone else's subscription.
     *
     * @param id      id of the subscription
     * @param request the request carrying the login token
     */


    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe
    (
        @PathVariable String id,
        HttpServletRequest request
    ) {
        subscriptions.unsubscribe(CurrentUser.of(request), id);
    }
}