/*
 * File:    VolunteerService.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Lists and adds volunteers, with two views: full records for coordinators and phone-free views for responders.
 */


package com.floodwatch.service.relief;

import com.floodwatch.model.Volunteer;
import com.floodwatch.model.VolunteerView;
import com.floodwatch.repository.VolunteerRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;


/**
 * Manages volunteers. The two read methods differ only in what they reveal: list returns the phone number and
 * views does not.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Service
public class VolunteerService {


    private static final String VOLUNTEER_ID_PREFIX = "vol-";

    private final VolunteerRepository volunteers;

    /**
     * Creates the service.
     *
     * @param volunteers volunteer storage
     */


    public VolunteerService
    (
        VolunteerRepository volunteers
    ) {
        this.volunteers = volunteers;
    }

    /**
     * Lists every volunteer with the phone number. Only coordinators and admins may call this.
     *
     * @return all volunteers
     */


    public List<Volunteer> list() {
        return volunteers.findAll();
    }

    /**
     * Lists every volunteer without the phone number, for responders.
     *
     * @return the phone-free view of every volunteer
     */


    public List<VolunteerView> views() {
        return volunteers.findAll().stream()
                .map(volunteer -> new VolunteerView(volunteer.id(), volunteer.name(), volunteer.skills(),
                        volunteer.teamId(), volunteer.available()))
                .toList();
    }

    /**
     * Adds a volunteer.
     *
     * @param name      display name
     * @param phone     contact number
     * @param skills    skills, null is stored as none
     * @param teamId    rescue team, or null
     * @param available true when the volunteer can be called now
     * @return the saved volunteer
     */


    public Volunteer create
    (
        String name, String phone,
        List<String> skills,
        String teamId,
        boolean available
    ) {
        List<String> skillList = skills == null ? List.of() : List.copyOf(skills);
        return volunteers.save(new Volunteer(VOLUNTEER_ID_PREFIX + UUID.randomUUID(), name, phone, skillList, teamId, available));
    }
}