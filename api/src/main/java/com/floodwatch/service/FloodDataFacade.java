package com.floodwatch.service;

import com.floodwatch.integration.RiverDataProvider;
import com.floodwatch.integration.WeatherDataProvider;
import com.floodwatch.model.Area;
import com.floodwatch.repository.AreaRepository;
import org.springframework.stereotype.Service;

/** Facade: one call gives the dashboard everything, hiding the weather, river (and later risk) classes. */
@Service
public class FloodDataFacade {

    static final String DISCLAIMER = "Modeled forecasts for learning only. Not an official warning; check PMD FFD bulletins.";
    private final AreaRepository areas;
    private final WeatherDataProvider weather;
    private final RiverDataProvider river;

    public FloodDataFacade(AreaRepository areas, WeatherDataProvider weather, RiverDataProvider river) {
        this.areas = areas;
        this.weather = weather;
        this.river = river;
    }

    public DashboardDto dashboardFor(String areaId) {
        Area area = areas.findById(areaId).orElseThrow(() -> new NotFoundException("Unknown area: " + areaId));
        return new DashboardDto(area, weather.hourlyRain(area), river.dailyDischarge(area), DISCLAIMER);
    }
}
