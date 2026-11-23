/*
 * File:    AreaAdminService.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Creates, changes and deletes areas for administrators, keeping each area's alert rule in step.
 */


package com.floodwatch.service.admin;

import com.floodwatch.controller.AreaRequest;
import com.floodwatch.controller.AreaUpdateRequest;
import com.floodwatch.model.AdminLimits;
import com.floodwatch.model.Area;
import com.floodwatch.model.AlertRule;
import com.floodwatch.model.IncidentLimits;
import com.floodwatch.model.RiskLevel;
import com.floodwatch.repository.AlertRuleRepository;
import com.floodwatch.repository.AreaRepository;
import com.floodwatch.repository.ShelterRepository;
import com.floodwatch.repository.SubscriptionRepository;
import com.floodwatch.service.BadRequestException;
import com.floodwatch.service.ConflictException;
import com.floodwatch.service.NotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;


/**
 * Admin area management. Creating an area also creates a disabled MEDIUM alert rule for it. Deleting an area is
 * refused while shelters, subscriptions or enabled alert rules still use it, and the message counts each kind.
 * A disabled rule does not count as use, because every new area starts with one.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Service
public class AreaAdminService {


    private static final Pattern AREA_ID = Pattern.compile("^[a-z0-9-]{2,40}$");

    private final AreaRepository areas;
    private final ShelterRepository shelters;
    private final SubscriptionRepository subscriptions;
    private final AlertRuleRepository rules;

    /**
     * Creates the service.
     *
     * @param areas         area storage
     * @param shelters      shelter storage, used to detect shelters that use an area
     * @param subscriptions subscription storage, used to detect subscriptions to an area
     * @param rules         alert rule storage
     */


    public AreaAdminService
    (
        AreaRepository areas,
        ShelterRepository shelters,
        SubscriptionRepository subscriptions,
        AlertRuleRepository rules
    ) {
        this.areas = areas;
        this.shelters = shelters;
        this.subscriptions = subscriptions;
        this.rules = rules;
    }

    /**
     * Creates an area and its disabled alert rule.
     *
     * @param request the area fields, including the new id
     * @return the saved area
     * @throws BadRequestException when the id or a field is invalid
     * @throws ConflictException   when an area with the id already exists
     */


    public synchronized Area create
    (
        AreaRequest request
    ) {
        String id = request.id();
        if (id == null || !AREA_ID.matcher(id).matches())
        {
            throw new BadRequestException("id must be 2 to 40 lowercase letters, digits or hyphens.");
        }
        Area area = build(id, new AreaUpdateRequest(request.name(), request.latitude(), request.longitude(), request.riverName()));
        if (areas.findById(id).isPresent())
        {
            throw new ConflictException("An area with this id already exists.");
        }
        Area saved = areas.save(area);
        rules.save(new AlertRule(id, RiskLevel.MEDIUM, false));
        return saved;
    }

    /**
     * Changes the name, coordinates or river of an existing area. Its alert rule is left as it is.
     *
     * @param id      id of the area
     * @param request the new area fields
     * @return the saved area
     * @throws NotFoundException   when the area does not exist
     * @throws BadRequestException when a field is invalid
     */


    public synchronized Area update
    (
        String id,
        AreaUpdateRequest request
    ) {
        requireArea(id);
        return areas.save(build(id, request));
    }

    /**
     * Deletes an area that nothing uses, together with its disabled alert rule.
     *
     * @param id id of the area
     * @throws NotFoundException when the area does not exist
     * @throws ConflictException when shelters, subscriptions or enabled alert rules still use the area
     */


    public synchronized void delete
    (
        String id
    ) {
        requireArea(id);
        List<String> uses = usages(id);
        if (!uses.isEmpty())
        {
            throw new ConflictException("Area is still used by " + joinWithAnd(uses) + ".");
        }
        areas.delete(id);
        rules.delete(id);
    }

    /**
     * Describes what still uses an area, such as "2 shelters" or "1 alert rule".
     *
     * @param areaId id of the area
     * @return one description per kind of use that is not zero, possibly empty
     */


    private List<String> usages
    (
        String areaId
    ) {
        long shelterCount = shelters.findAll().stream().filter(s -> s.areaId().equals(areaId)).count();
        long subscriptionCount = subscriptions.findAll().stream().filter(s -> s.areaId().equals(areaId)).count();
        long ruleCount = rules.findAll().stream().filter(r -> r.areaId().equals(areaId) && r.enabled()).count();
        List<String> uses = new ArrayList<>();
        if (shelterCount > 0)
        {
            uses.add(describe(shelterCount, "shelter", "shelters"));
        }
        if (subscriptionCount > 0)
        {
            uses.add(describe(subscriptionCount, "subscription", "subscriptions"));
        }
        if (ruleCount > 0)
        {
            uses.add(describe(ruleCount, "alert rule", "alert rules"));
        }
        return uses;
    }

    /**
     * Writes a count with the right singular or plural word.
     *
     * @param count    how many
     * @param singular the word for one
     * @param plural   the word for any other number
     * @return the count and word, such as "2 shelters"
     */


    private static String describe
    (
        long count,
        String singular, String plural
    ) {
        return count + " " + (count == 1 ? singular : plural);
    }

    /**
     * Joins the parts with commas, using "and" before the last one.
     *
     * @param parts the parts to join, at least one
     * @return the joined text, such as "2 shelters, 1 subscription and 1 alert rule"
     */


    private static String joinWithAnd
    (
        List<String> parts
    ) {
        if (parts.size() == 1)
        {
            return parts.get(0);
        }
        String last = parts.get(parts.size() - 1);
        return String.join(", ", parts.subList(0, parts.size() - 1)) + " and " + last;
    }

    /**
     * Checks the fields and builds the area that will be saved under the given id.
     *
     * @param id      id the area will have
     * @param request the fields sent by the caller
     * @return the checked area, not yet saved
     */


    private static Area build
    (
        String id,
        AreaUpdateRequest request
    ) {
        String name = AdminValidation.text(request.name(), "name", AdminLimits.NAME_MIN, AdminLimits.NAME_MAX);
        double latitude = AdminValidation.coordinate(request.latitude(), "latitude", IncidentLimits.LATITUDE_MIN, IncidentLimits.LATITUDE_MAX);
        double longitude = AdminValidation.coordinate(request.longitude(), "longitude", IncidentLimits.LONGITUDE_MIN, IncidentLimits.LONGITUDE_MAX);
        String riverName = AdminValidation.text(request.riverName(), "riverName", AdminLimits.RIVER_NAME_MIN, AdminLimits.RIVER_NAME_MAX);
        return new Area(id, name, latitude, longitude, riverName);
    }

    /**
     * Finds an area or fails with 404.
     *
     * @param id id of the area
     * @return the area
     * @throws NotFoundException when the area does not exist
     */


    private Area requireArea
    (
        String id
    ) {
        return areas.findById(id).orElseThrow(() -> new NotFoundException("Unknown area: " + id));
    }
}