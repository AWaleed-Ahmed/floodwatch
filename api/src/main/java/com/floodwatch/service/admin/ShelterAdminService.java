/*
 * File:    ShelterAdminService.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Creates, changes and deletes shelters for administrators, with validation and duplicate checks.
 */


package com.floodwatch.service.admin;

import com.floodwatch.controller.ShelterRequest;
import com.floodwatch.model.AdminLimits;
import com.floodwatch.model.IncidentLimits;
import com.floodwatch.model.Shelter;
import com.floodwatch.repository.AreaRepository;
import com.floodwatch.repository.ShelterRepository;
import com.floodwatch.service.BadRequestException;
import com.floodwatch.service.ConflictException;
import com.floodwatch.service.NotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;


/**
 * Admin shelter management. Checks run in this order: fields (400), unknown area (404), duplicate name in
 * the area (409). Writes are synchronized so two requests cannot both pass the duplicate check.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Service
public class ShelterAdminService {


    private static final String SHELTER_ID_PREFIX = "sh-";

    private final ShelterRepository shelters;
    private final AreaRepository areas;

    /**
     * Creates the service.
     *
     * @param shelters shelter storage
     * @param areas    area storage, used to reject unknown areas
     */


    public ShelterAdminService
    (
        ShelterRepository shelters,
        AreaRepository areas
    ) {
        this.shelters = shelters;
        this.areas = areas;
    }

    /**
     * Creates a shelter with a new id.
     *
     * @param request the shelter fields
     * @return the saved shelter
     * @throws BadRequestException when a field is invalid
     * @throws NotFoundException   when the area does not exist
     * @throws ConflictException   when the area already has a shelter with the same name
     */


    public synchronized Shelter create
    (
        ShelterRequest request
    ) {
        return shelters.save(build(SHELTER_ID_PREFIX + UUID.randomUUID(), request));
    }

    /**
     * Replaces the fields of an existing shelter.
     *
     * @param id      id of the shelter
     * @param request the new shelter fields
     * @return the saved shelter
     * @throws NotFoundException   when the shelter or the area does not exist
     * @throws BadRequestException when a field is invalid
     * @throws ConflictException   when the area already has another shelter with the same name
     */


    public synchronized Shelter update
    (
        String id,
        ShelterRequest request
    ) {
        requireShelter(id);
        return shelters.save(build(id, request));
    }

    /**
     * Deletes a shelter.
     *
     * @param id id of the shelter
     * @throws NotFoundException when the shelter does not exist
     */


    public synchronized void delete
    (
        String id
    ) {
        requireShelter(id);
        shelters.delete(id);
    }

    /**
     * Checks the fields and builds the shelter that will be saved under the given id.
     *
     * @param id      id the shelter will have
     * @param request the fields sent by the caller
     * @return the checked shelter, not yet saved
     */


    private Shelter build
    (
        String id,
        ShelterRequest request
    ) {
        String areaId = AdminValidation.text(request.areaId(), "areaId", 1, AdminLimits.AREA_REFERENCE_MAX);
        String name = AdminValidation.text(request.name(), "name", AdminLimits.NAME_MIN, AdminLimits.NAME_MAX);
        double latitude = AdminValidation.coordinate(request.latitude(), "latitude", IncidentLimits.LATITUDE_MIN, IncidentLimits.LATITUDE_MAX);
        double longitude = AdminValidation.coordinate(request.longitude(), "longitude", IncidentLimits.LONGITUDE_MIN, IncidentLimits.LONGITUDE_MAX);
        int capacity = AdminValidation.capacity(request.capacity());
        if (request.status() == null)
        {
            throw new BadRequestException("status is required.");
        }
        String contact = AdminValidation.optionalText(request.contact(), "contact", AdminLimits.CONTACT_MAX);
        boolean sample = request.sample() == null || request.sample();
        if (areas.findById(areaId).isEmpty())
        {
            throw new NotFoundException("Unknown area: " + areaId);
        }
        boolean duplicate = shelters.findAll().stream()
                .anyMatch(s -> s.areaId().equals(areaId) && !s.id().equals(id) && s.name().equalsIgnoreCase(name));
        if (duplicate)
        {
            throw new ConflictException("A shelter with this name already exists in this area.");
        }
        return new Shelter(id, areaId, name, latitude, longitude, request.status(), contact, capacity, sample);
    }

    /**
     * Finds a shelter or fails with 404.
     *
     * @param id id of the shelter
     * @return the shelter
     * @throws NotFoundException when the shelter does not exist
     */


    private Shelter requireShelter
    (
        String id
    ) {
        return shelters.findById(id).orElseThrow(() -> new NotFoundException("Unknown shelter: " + id));
    }
}