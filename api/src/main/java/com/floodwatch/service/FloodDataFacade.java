/*
 * File:    FloodDataFacade.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Facade that gives the dashboard all of its data in one call.
 */


package com.floodwatch.service;

import com.floodwatch.integration.RiverDataProvider;
import com.floodwatch.integration.WeatherDataProvider;
import com.floodwatch.model.Area;
import com.floodwatch.repository.AreaRepository;
import org.springframework.stereotype.Service;


/**
 * Facade: one call gives the dashboard everything, hiding the weather and river classes behind it.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Service
public class FloodDataFacade {


    static final String DISCLAIMER = "Modeled forecasts for learning only. Not an official warning; check PMD FFD bulletins.";

    private final AreaRepository areas;
    private final WeatherDataProvider weather;
    private final RiverDataProvider river;

    /**
     * Creates the facade.
     *
     * @param areas   area storage
     * @param weather rainfall source (the caching proxy)
     * @param river   discharge source (the caching proxy)
     */


    public FloodDataFacade
    (
        AreaRepository areas,
        WeatherDataProvider weather,
        RiverDataProvider river
    ) {
        this.areas = areas;
        this.weather = weather;
        this.river = river;
    }

    /**
     * Builds the dashboard for one area.
     *
     * @param areaId id of the selected area
     * @return rainfall, discharge and the disclaimer for the area
     * @throws NotFoundException when the area does not exist
     */


    public DashboardDto dashboardFor
    (
        String areaId
    ) {
        Area area = areas.findById(areaId).orElseThrow(() -> new NotFoundException("Unknown area: " + areaId));
        return new DashboardDto(area, weather.hourlyRain(area), river.dailyDischarge(area), DISCLAIMER);
    }
}