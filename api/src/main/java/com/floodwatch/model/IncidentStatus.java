package com.floodwatch.model;

/** Incident life cycle from the Phase 2 state machine: OPEN -> ASSIGNED -> RESOLVED. */
public enum IncidentStatus {
    OPEN, ASSIGNED, RESOLVED;

    public boolean canMoveTo(IncidentStatus next) {
        return switch (this) {
            case OPEN -> next == ASSIGNED || next == RESOLVED; // OPEN -> RESOLVED is admin-only (checked in the service)
            case ASSIGNED -> next == ASSIGNED || next == RESOLVED;
            case RESOLVED -> false;
        };
    }
}
