/*
 * File:    IncidentBuilderTest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Unit tests for Incident.Builder: required fields, defaults, copied history and toBuilder.
 */


package com.floodwatch.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;


/**
 * Checks that Incident.Builder refuses missing required fields and fills in the documented defaults.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


class IncidentBuilderTest {


    private static final Instant CREATED = Instant.parse("2026-09-01T06:00:00Z");

    /**
     * Builds a builder with every required field set.
     *
     * @return a complete builder
     */


    private Incident.Builder completeBuilder() {
        return new Incident.Builder()
                .id("inc-test")
                .title("Road under water")
                .type(IncidentType.FLOODED_ROAD)
                .latitude(34.77)
                .longitude(72.36)
                .status(IncidentStatus.OPEN)
                .createdBy("user-resident1")
                .createdAt(CREATED);
    }

    /**
     * Builds a builder that leaves out the latitude, which is required.
     *
     * @return a builder without a latitude
     */


    private Incident.Builder builderWithoutLatitude() {
        return new Incident.Builder()
                .id("inc-test")
                .title("Road under water")
                .type(IncidentType.FLOODED_ROAD)
                .longitude(72.36)
                .status(IncidentStatus.OPEN)
                .createdBy("user-resident1")
                .createdAt(CREATED);
    }

    /**
     * Leaving out optional fields gives version 1, empty description, no team and updatedAt equal to createdAt.
     */


    @Test
    void buildsDefaultsWhenOptionalFieldsAreMissing() {
        Incident incident = completeBuilder().build();
        assertEquals(1L, incident.version());
        assertEquals("", incident.description());
        assertEquals(CREATED, incident.updatedAt());
        assertNull(incident.assignedTeamId());
        assertTrue(incident.history().isEmpty());
    }

    /**
     * A missing title is refused with IllegalStateException.
     */


    @Test
    void missingTitleIsRefused() {
        assertThrows(IllegalStateException.class, () -> completeBuilder().title(null).build());
    }

    /**
     * A missing type is refused with IllegalStateException.
     */


    @Test
    void missingTypeIsRefused() {
        assertThrows(IllegalStateException.class, () -> completeBuilder().type(null).build());
    }

    /**
     * A missing latitude is refused with IllegalStateException.
     */


    @Test
    void missingLatitudeIsRefused() {
        assertThrows(IllegalStateException.class, () -> builderWithoutLatitude().build());
    }

    /**
     * The history is copied when the incident is built, so later changes to the caller's list do not show up,
     * and the incident's own list cannot be changed.
     */


    @Test
    void historyIsCopiedAndCannotBeChanged() {
        List<StatusChange> changes = new ArrayList<>();
        changes.add(new StatusChange(null, IncidentStatus.OPEN, null, "user-resident1", CREATED));
        Incident incident = completeBuilder().history(changes).build();
        changes.clear();
        assertEquals(1, incident.history().size());
        assertThrows(UnsupportedOperationException.class,
                () -> incident.history().add(new StatusChange(null, IncidentStatus.OPEN, null, "x", CREATED)));
    }

    /**
     * toBuilder makes a changed copy and leaves the original incident unchanged.
     */


    @Test
    void toBuilderMakesAChangedCopyAndLeavesTheOriginal() {
        Incident original = completeBuilder().build();
        Incident changed = original.toBuilder().status(IncidentStatus.ASSIGNED).version(2L).build();
        assertEquals(IncidentStatus.OPEN, original.status());
        assertEquals(1L, original.version());
        assertEquals(IncidentStatus.ASSIGNED, changed.status());
        assertEquals(2L, changed.version());
        assertEquals(original.id(), changed.id());
    }
}