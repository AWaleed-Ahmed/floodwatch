/*
 * File:    TeamController.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: REST endpoint that lists the rescue teams incidents can be assigned to.
 */


package com.floodwatch.controller;

import com.floodwatch.model.Role;
import com.floodwatch.model.Team;
import com.floodwatch.security.RequiresRole;
import com.floodwatch.service.incidents.TeamService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * Lists rescue teams for responders, coordinators and admins.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@RestController
@RequestMapping("/api/teams")
public class TeamController {


    private final TeamService teams;

    /**
     * Creates the controller.
     *
     * @param teams the team service
     */


    public TeamController
    (
        TeamService teams
    ) {
        this.teams = teams;
    }

    /**
     * Returns every rescue team.
     *
     * @return all teams
     */


    @GetMapping
    @RequiresRole({Role.RESPONDER, Role.COORDINATOR, Role.ADMIN})
    public List<Team> list() {
        return teams.list();
    }
}