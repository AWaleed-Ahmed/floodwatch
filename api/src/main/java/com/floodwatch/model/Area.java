package com.floodwatch.model;

/** A place residents can select. Seeded in data/areas.json. */
public record Area(String id, String name, double latitude, double longitude, String riverName) {}
