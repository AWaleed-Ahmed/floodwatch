/*
 * File:    RiskRule.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Base class of the risk chain: each rule decides a level or passes the case on.
 */


package com.floodwatch.service.risk;

import java.util.Optional;


/**
 * One link in the Chain of Responsibility that decides a risk level. A rule either decides
 * or hands the same context to the next rule, so the order of the chain sets the priority.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


public abstract class RiskRule {


    private final RiskRule next;

    /**
     * Creates the rule with the rule that runs when this one passes.
     *
     * @param next the following rule, or null for the last rule in the chain
     */


    protected RiskRule
    (
        RiskRule next
    ) {
        this.next = next;
    }

    /**
     * Runs this rule and, when it passes, the rest of the chain.
     *
     * @param context the numbers to judge
     * @return the first decision made along the chain
     * @throws IllegalStateException when no rule in the chain decides
     */


    public RiskDecision evaluate
    (
        RiskContext context
    ) {
        Optional<RiskDecision> decision = decide(context);
        if (decision.isPresent())
        {
            return decision.get();
        }
        if (next == null)
        {
            throw new IllegalStateException("No risk rule decided the level.");
        }
        return next.evaluate(context);
    }

    /**
     * Decides a level for this rule, or returns empty to pass the context on.
     *
     * @param context the numbers to judge
     * @return the decision, or empty when this rule does not apply
     */


    protected abstract Optional<RiskDecision> decide
    (
        RiskContext context
    );
}