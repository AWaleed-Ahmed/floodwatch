package com.floodwatch.model;

/** A shelter pin on the map. sample = true means it is demo data, not a verified shelter. */
public record Shelter(String id, String areaId, String name, double latitude, double longitude,
                      ShelterStatus status, String contact, int capacity, boolean sample) {

    public String directionsUrl() {
        return "https://www.openstreetmap.org/directions?route=%3B" + latitude + "%2C" + longitude;
    }
}
