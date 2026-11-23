/*
 * File:    Baseline.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Stored typical monsoon river flow for one area, built from past seasons.
 */


package com.floodwatch.model;

import java.time.Instant;


/**
 * Median July-September river discharge for one area over the last complete monsoon seasons.
 *
 * @param areaId     id of the area the baseline belongs to
 * @param median     median daily discharge in m3/s over the monsoon days
 * @param sampleCount number of non-null daily values the median was taken from
 * @param firstYear  first season year included
 * @param lastYear   last season year included
 * @param computedAt when the baseline was computed
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record Baseline
(
    String areaId,
    double median,
    int sampleCount, int firstYear, int lastYear,
    Instant computedAt
) {


}