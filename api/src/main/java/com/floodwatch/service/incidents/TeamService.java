/*
 * File:    TeamService.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Lists the rescue teams that incidents can be assigned to.
 */


package com.floodwatch.service.incidents;

import com.floodwatch.model.Team;
import com.floodwatch.repository.TeamRepository;
import java.util.List;
import org.springframework.stereotype.Service;


/**
 * Lists rescue teams.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Service
public class TeamService {


    private final TeamRepository teams;

    /**
     * Creates the service.
     *
     * @param teams team storage
     */


    public TeamService
    (
        TeamRepository teams
    ) {
        this.teams = teams;
    }

    /**
     * Returns every rescue team.
     *
     * @return all teams
     */


    public List<Team> list() {
        return teams.findAll();
    }
}