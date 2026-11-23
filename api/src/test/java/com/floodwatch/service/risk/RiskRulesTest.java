/*
 * File:    RiskRulesTest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Tests the boundaries and order of the risk rule chain.
 */


package com.floodwatch.service.risk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.floodwatch.model.RiskLevel;
import org.junit.jupiter.api.Test;


/**
 * Checks each rule's limits exactly, including the values on the boundary, and the order of the chain.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


class RiskRulesTest {


    /**
     * Builds the production chain: high discharge, then heavy rain, then the LOW default.
     *
     * @return the first rule of the chain
     */


    private static RiskRule chain() {
        return new HighDischargeRule(new HeavyRainRule(new BaselineDefaultRule()));
    }

    /**
     * Runs the production chain on one set of numbers.
     *
     * @param discharge forecast discharge in m3/s
     * @param baseline  typical discharge in m3/s, or null
     * @param rainMm    rainfall in mm, or null when unknown
     * @return the decision
     */


    private static RiskDecision decide
    (
        double discharge,
        Double baseline, Double rainMm
    ) {
        return chain().evaluate(new RiskContext(discharge, baseline, rainMm));
    }

    /**
     * A ratio of 1.29 and exactly 1.30 are both LOW; the rule uses strict greater-than.
     */


    @Test
    void ratioAtOrBelowOnePointThreeIsLow() {
        assertEquals(RiskLevel.LOW, decide(129.0, 100.0, 0.0).level());
        assertEquals(RiskLevel.LOW, decide(130.0, 100.0, 0.0).level());
    }

    /**
     * A ratio just above 1.30 is MEDIUM, and exactly 2.00 is still MEDIUM.
     */


    @Test
    void ratioJustAboveOnePointThreeIsMediumAndExactlyTwoIsStillMedium() {
        assertEquals(RiskLevel.MEDIUM, decide(131.0, 100.0, 0.0).level());
        assertEquals(RiskLevel.MEDIUM, decide(200.0, 100.0, 0.0).level());
    }

    /**
     * A ratio of 2.01 is HIGH, and the reason states the ratio in plain words.
     */


    @Test
    void ratioAboveTwoIsHighWithAPlainReason() {
        RiskDecision decision = decide(201.0, 100.0, 0.0);
        assertEquals(RiskLevel.HIGH, decision.level());
        assertEquals("River flow is 2.0× the normal monsoon level.", decision.reason());
    }

    /**
     * The reason for a ratio of 2.3 matches the documented example text.
     */


    @Test
    void reasonForTwoPointThreeMatchesTheExample() {
        assertEquals("River flow is 2.3× the normal monsoon level.", decide(230.0, 100.0, 0.0).reason());
    }

    /**
     * Rain of exactly 50 mm is not heavy; 50.1 mm is MEDIUM.
     */


    @Test
    void rainOfFiftyIsNotHeavyButFiftyPointOneIsMedium() {
        assertEquals(RiskLevel.LOW, decide(100.0, 100.0, 50.0).level());
        assertEquals(RiskLevel.MEDIUM, decide(100.0, 100.0, 50.1).level());
    }

    /**
     * High discharge is checked before heavy rain, so a HIGH ratio gives HIGH even with heavy rain.
     */


    @Test
    void highDischargeWinsOverHeavyRain() {
        RiskDecision decision = decide(230.0, 100.0, 80.0);
        assertEquals(RiskLevel.HIGH, decision.level());
        assertFalse(decision.reason().contains("Rainfall"));
    }

    /**
     * When both a raised ratio and heavy rain apply, the MEDIUM reason gives both facts.
     */


    @Test
    void mediumReasonGivesEveryFactThatApplies() {
        RiskDecision decision = decide(150.0, 100.0, 60.0);
        assertEquals(RiskLevel.MEDIUM, decision.level());
        assertEquals("River flow is 1.5× the normal monsoon level. Rainfall of 60.0 mm is above 50 mm.", decision.reason());
    }

    /**
     * With no baseline, heavy rain alone gives MEDIUM and the reason says the baseline is missing.
     */


    @Test
    void missingBaselineJudgesHeavyRainOnly() {
        RiskDecision decision = decide(999.0, null, 60.0);
        assertEquals(RiskLevel.MEDIUM, decision.level());
        assertTrue(decision.reason().startsWith("No river baseline yet; judged on rainfall only."));
    }

    /**
     * With no baseline and light rain, the default rule gives LOW with the exact missing-baseline reason.
     */


    @Test
    void missingBaselineWithLightRainIsLowWithTheStandardReason() {
        RiskDecision decision = decide(999.0, null, 10.0);
        assertEquals(RiskLevel.LOW, decision.level());
        assertEquals("No river baseline yet; judged on rainfall only.", decision.reason());
    }

    /**
     * With a usable baseline and no trigger, the default rule gives LOW and names the ratio.
     */


    @Test
    void normalFlowAndLightRainIsLow() {
        RiskDecision decision = decide(110.0, 100.0, 5.0);
        assertEquals(RiskLevel.LOW, decision.level());
        assertEquals("River flow is 1.1× the normal monsoon level, below the warning levels.", decision.reason());
    }

    /**
     * A rule with no next rule that does not decide fails loudly instead of returning nothing.
     */


    @Test
    void chainWithoutADecidingRuleFails() {
        RiskRule lone = new HeavyRainRule(null);
        assertThrows(IllegalStateException.class, () -> lone.evaluate(new RiskContext(100.0, 100.0, 0.0)));
    }
}