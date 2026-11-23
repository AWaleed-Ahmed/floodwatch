package com.floodwatch.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** State machine rules from Phase 2, Figure 8. */
class IncidentStatusTest {

    @Test
    void legalMoves() {
        assertTrue(IncidentStatus.OPEN.canMoveTo(IncidentStatus.ASSIGNED));
        assertTrue(IncidentStatus.ASSIGNED.canMoveTo(IncidentStatus.RESOLVED));
        assertTrue(IncidentStatus.ASSIGNED.canMoveTo(IncidentStatus.ASSIGNED));
    }

    @Test
    void resolvedIsFinal() {
        for (IncidentStatus next : IncidentStatus.values()) {
            assertFalse(IncidentStatus.RESOLVED.canMoveTo(next));
        }
    }
}
