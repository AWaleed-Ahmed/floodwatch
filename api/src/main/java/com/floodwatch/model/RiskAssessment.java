/*
 * File:    RiskAssessment.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: One educational flood risk result for an area and a day.
 */


package com.floodwatch.model;

import java.time.LocalDate;


/**
 * Educational flood risk for one area and one day. A null level means the level could not be decided.
 *
 * @param areaId     id of the area
 * @param date       the day assessed, in Pakistan time
 * @param level      LOW, MEDIUM or HIGH; null when the data is not trustworthy enough to decide
 * @param discharge  forecast river discharge for the day in m3/s; null when unavailable
 * @param baseline   typical monsoon discharge in m3/s; null when no baseline exists
 * @param ratio      discharge divided by baseline; null when either is unknown
 * @param rainMm     rainfall total for the day in mm; null when unknown
 * @param reason     plain-language explanation of the level
 * @param dataStatus the weaker status of the discharge and rainfall series
 * @param disclaimer warning that this is not an official alert
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record RiskAssessment
(
    String areaId,
    LocalDate date,
    RiskLevel level,
    Double discharge, Double baseline, Double ratio, Double rainMm,
    String reason,
    DataStatus dataStatus,
    String disclaimer
) {


}