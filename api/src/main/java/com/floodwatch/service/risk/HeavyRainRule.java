/*
 * File:    HeavyRainRule.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Second rule: raised river flow or heavy daily rain gives MEDIUM risk.
 */


package com.floodwatch.service.risk;

import com.floodwatch.model.RiskLevel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;


/**
 * Gives MEDIUM risk when discharge is more than 1.3 times the baseline, or when the day's rain is above 50 mm.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public class HeavyRainRule extends RiskRule {


    static final double MEDIUM_RATIO = 1.3;
    static final double HEAVY_RAIN_MM = 50.0;
    static final String NO_BASELINE = "No river baseline yet; judged on rainfall only.";

    /**
     * Creates the rule.
     *
     * @param next the rule to run when this one passes
     */


    public HeavyRainRule
    (
        RiskRule next
    ) {
        super(next);
    }

    /**
     * Decides MEDIUM when the ratio is above {@value #MEDIUM_RATIO} or the rain is above {@value #HEAVY_RAIN_MM} mm.
     *
     * @param context the numbers to judge
     * @return a MEDIUM decision with every reason that applies, or empty when neither limit is passed
     */


    @Override
    protected Optional<RiskDecision> decide
    (
        RiskContext context
    ) {
        List<String> reasons = new ArrayList<>();
        if (context.hasBaseline() && context.ratio() > MEDIUM_RATIO)
        {
            reasons.add(String.format(Locale.ROOT, "River flow is %.1f× the normal monsoon level.", context.ratio()));
        }
        if (context.rainMm() != null && context.rainMm() > HEAVY_RAIN_MM)
        {
            reasons.add(String.format(Locale.ROOT, "Rainfall of %.1f mm is above %.0f mm.", context.rainMm(), HEAVY_RAIN_MM));
        }
        if (reasons.isEmpty())
        {
            return Optional.empty();
        }
        if (!context.hasBaseline())
        {
            reasons.add(0, NO_BASELINE);
        }
        return Optional.of(new RiskDecision(RiskLevel.MEDIUM, String.join(" ", reasons)));
    }
}