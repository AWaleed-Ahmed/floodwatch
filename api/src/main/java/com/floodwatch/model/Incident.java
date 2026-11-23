/*
 * File:    Incident.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: An emergency incident with its status history, built step by step with Incident.Builder.
 */


package com.floodwatch.model;

import java.time.Instant;
import java.util.List;


/**
 * An emergency incident. Records cannot change, so a status change builds a new copy with toBuilder()
 * and leaves the old copy alone. The history is copied when it is built and only ever grows.
 *
 * @param id             unique id, such as inc- followed by a UUID
 * @param title          short title, 3 to 120 characters
 * @param type           kind of incident
 * @param latitude       latitude inside Pakistan
 * @param longitude      longitude inside Pakistan
 * @param description    details, may be empty
 * @param status         current life cycle state
 * @param assignedTeamId team the incident is assigned to, or null before assignment
 * @param createdBy      id of the user who reported the incident
 * @param createdAt      when the incident was reported
 * @param updatedAt      when the incident last changed
 * @param version        number of changes so far, starting at 1; used for optimistic locking
 * @param history        every status change, in time order
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public record Incident
(
    String id, String title,
    IncidentType type,
    double latitude, double longitude,
    String description,
    IncidentStatus status,
    String assignedTeamId, String createdBy,
    Instant createdAt, Instant updatedAt,
    long version,
    List<StatusChange> history
) {


    /**
     * Starts a builder with every value of this incident, so a changed copy can be made.
     *
     * @return a builder holding the values of this incident
     */


    public Builder toBuilder() {
        return new Builder()
                .id(id)
                .title(title)
                .type(type)
                .latitude(latitude)
                .longitude(longitude)
                .description(description)
                .status(status)
                .assignedTeamId(assignedTeamId)
                .createdBy(createdBy)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .version(version)
                .history(history);
    }


    /**
     * Builds an incident step by step. build() checks the required fields and copies the history.
     * version defaults to 1, updatedAt defaults to createdAt and description defaults to empty text.
     *
     * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
     */


    public static final class Builder {


        private String id;
        private String title;
        private IncidentType type;
        private Double latitude;
        private Double longitude;
        private String description;
        private IncidentStatus status;
        private String assignedTeamId;
        private String createdBy;
        private Instant createdAt;
        private Instant updatedAt;
        private long version = 1L;
        private List<StatusChange> history = List.of();

        /**
         * Sets the incident id.
         *
         * @param value the id
         * @return this builder
         */


        public Builder id
        (
            String value
        ) {
            this.id = value;
            return this;
        }

        /**
         * Sets the title.
         *
         * @param value the title
         * @return this builder
         */


        public Builder title
        (
            String value
        ) {
            this.title = value;
            return this;
        }

        /**
         * Sets the incident type.
         *
         * @param value the type
         * @return this builder
         */


        public Builder type
        (
            IncidentType value
        ) {
            this.type = value;
            return this;
        }

        /**
         * Sets the latitude.
         *
         * @param value the latitude
         * @return this builder
         */


        public Builder latitude
        (
            double value
        ) {
            this.latitude = value;
            return this;
        }

        /**
         * Sets the longitude.
         *
         * @param value the longitude
         * @return this builder
         */


        public Builder longitude
        (
            double value
        ) {
            this.longitude = value;
            return this;
        }

        /**
         * Sets the description.
         *
         * @param value the description, null is stored as empty text
         * @return this builder
         */


        public Builder description
        (
            String value
        ) {
            this.description = value;
            return this;
        }

        /**
         * Sets the status.
         *
         * @param value the status
         * @return this builder
         */


        public Builder status
        (
            IncidentStatus value
        ) {
            this.status = value;
            return this;
        }

        /**
         * Sets the assigned team id.
         *
         * @param value the team id, or null when no team is assigned
         * @return this builder
         */


        public Builder assignedTeamId
        (
            String value
        ) {
            this.assignedTeamId = value;
            return this;
        }

        /**
         * Sets the id of the user who reported the incident.
         *
         * @param value the user id
         * @return this builder
         */


        public Builder createdBy
        (
            String value
        ) {
            this.createdBy = value;
            return this;
        }

        /**
         * Sets the time the incident was reported.
         *
         * @param value the time
         * @return this builder
         */


        public Builder createdAt
        (
            Instant value
        ) {
            this.createdAt = value;
            return this;
        }

        /**
         * Sets the time of the last change.
         *
         * @param value the time, null means the same as createdAt
         * @return this builder
         */


        public Builder updatedAt
        (
            Instant value
        ) {
            this.updatedAt = value;
            return this;
        }

        /**
         * Sets the version number.
         *
         * @param value the version
         * @return this builder
         */


        public Builder version
        (
            long value
        ) {
            this.version = value;
            return this;
        }

        /**
         * Sets the status history. The list is copied when the incident is built.
         *
         * @param value the history entries in time order
         * @return this builder
         */


        public Builder history
        (
            List<StatusChange> value
        ) {
            this.history = value;
            return this;
        }

        /**
         * Checks the required fields and creates the incident.
         *
         * @return the new incident
         * @throws IllegalStateException when id, title, type, latitude, longitude, status, createdBy or createdAt is missing
         */


        public Incident build() {
            require(id, "id");
            require(title, "title");
            require(type, "type");
            require(latitude, "latitude");
            require(longitude, "longitude");
            require(status, "status");
            require(createdBy, "createdBy");
            require(createdAt, "createdAt");
            Instant stamp = updatedAt == null ? createdAt : updatedAt;
            String text = description == null ? "" : description;
            return new Incident(id, title, type, latitude, longitude, text, status,
                    assignedTeamId, createdBy, createdAt, stamp, version, List.copyOf(history));
        }

        /**
         * Fails when a required value is missing.
         *
         * @param value the value to check
         * @param name  the field name used in the message
         * @throws IllegalStateException when the value is null
         */


        private static void require
        (
            Object value,
            String name
        ) {
            if (value == null)
            {
                throw new IllegalStateException("Incident " + name + " is required.");
            }
        }
    }
}