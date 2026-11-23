/*
 * File:    BaselineDefaultRule.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Last rule: when no earlier rule applies, the level is LOW.
 */


package com.floodwatch.service.risk;

import com.floodwatch.model.RiskLevel;
import java.util.Locale;
import java.util.Optional;


/**
 * Final rule of the chain: always decides LOW, with a reason that says whether a baseline was used.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public class BaselineDefaultRule extends RiskRule {


    /**
     * Creates the last rule, which has no rule after it.
     */


    public BaselineDefaultRule() {
        super(null);
    }

    /**
     * Always decides LOW.
     *
     * @param context the numbers to judge
     * @return a LOW decision
     */


    @Override
    protected Optional<RiskDecision> decide
    (
        RiskContext context
    ) {
        String reason = HeavyRainRule.NO_BASELINE;
        if (context.hasBaseline())
        {
            reason = String.format(Locale.ROOT, "River flow is %.1f× the normal monsoon level, below the warning levels.", context.ratio());
        }
        return Optional.of(new RiskDecision(RiskLevel.LOW, reason));
    }
}