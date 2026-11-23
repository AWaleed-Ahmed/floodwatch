package com.floodwatch.service;

import com.floodwatch.model.Area;
import com.floodwatch.model.ForecastSeries;

public record DashboardDto(Area area, ForecastSeries rain, ForecastSeries discharge, String disclaimer) {}
