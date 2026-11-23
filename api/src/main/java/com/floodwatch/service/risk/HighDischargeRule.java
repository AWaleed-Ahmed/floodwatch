/*
 * File:    HighDischargeRule.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: First rule: river flow far above the monsoon norm gives HIGH risk.
 */


package com.floodwatch.service.risk;

import com.floodwatch.model.RiskLevel;
import java.util.Locale;
import java.util.Optional;


/**
 * Gives HIGH risk when discharge is more than twice the baseline.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public class HighDischargeRule extends RiskRule {


    static final double HIGH_RATIO = 2.0;

    /**
     * Creates the rule.
     *
     * @param next the rule to run when this one passes
     */


    public HighDischargeRule
    (
        RiskRule next
    ) {
        super(next);
    }

    /**
     * Decides HIGH when the discharge ratio is strictly above {@value #HIGH_RATIO}.
     *
     * @param context the numbers to judge
     * @return a HIGH decision, or empty when the ratio is not above the limit or no baseline exists
     */


    @Override
    protected Optional<RiskDecision> decide
    (
        RiskContext context
    ) {
        if (!context.hasBaseline() || context.ratio() <= HIGH_RATIO)
        {
            return Optional.empty();
        }
        String reason = String.format(Locale.ROOT, "River flow is %.1f× the normal monsoon level.", context.ratio());
        return Optional.of(new RiskDecision(RiskLevel.HIGH, reason));
    }
}