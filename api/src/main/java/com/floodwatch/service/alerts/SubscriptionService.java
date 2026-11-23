/*
 * File:    SubscriptionService.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Lets a resident follow areas, changes the minimum level and stops following an area.
 */


package com.floodwatch.service.alerts;

import com.floodwatch.config.AppProperties;
import com.floodwatch.model.RiskLevel;
import com.floodwatch.model.Subscription;
import com.floodwatch.model.User;
import com.floodwatch.repository.AreaRepository;
import com.floodwatch.repository.SubscriptionRepository;
import com.floodwatch.service.BusinessRuleException;
import com.floodwatch.service.NotFoundException;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;


/**
 * Feature 3 subscriptions. A user has at most one subscription per area, and at most maxSubscriptionsPerUser in total.
 * Writes are synchronized so two quick requests cannot create duplicate subscriptions for one area.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@Service
public class SubscriptionService {


    private static final RiskLevel DEFAULT_LEVEL = RiskLevel.MEDIUM;
    private static final String UNKNOWN_SUBSCRIPTION = "Subscription not found.";

    private final SubscriptionRepository subscriptions;
    private final AreaRepository areas;
    private final AppProperties props;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param subscriptions subscription storage
     * @param areas         area storage, used to reject unknown areas
     * @param props         application settings holding the subscription limit
     * @param clock         source of the current time
     */


    public SubscriptionService
    (
        SubscriptionRepository subscriptions,
        AreaRepository areas,
        AppProperties props,
        Clock clock
    ) {
        this.subscriptions = subscriptions;
        this.areas = areas;
        this.props = props;
        this.clock = clock;
    }

    /**
     * Follows an area. Following the same area again returns the existing subscription and updates its minimum
     * level when it differs. A missing minimum level means MEDIUM.
     *
     * @param user     the user who follows the area
     * @param areaId   id of the area to follow
     * @param minLevel lowest level that alerts this user, or null for MEDIUM
     * @return the subscription, and whether it was newly created
     * @throws NotFoundException       when the area does not exist
     * @throws BusinessRuleException   when the user already follows the maximum number of areas
     */


    public synchronized Result subscribe
    (
        User user,
        String areaId,
        RiskLevel minLevel
    ) {
        RiskLevel level = minLevel == null ? DEFAULT_LEVEL : minLevel;
        if (areas.findById(areaId).isEmpty())
        {
            throw new NotFoundException("Unknown area: " + areaId);
        }
        List<Subscription> current = mine(user);
        Optional<Subscription> existing = current.stream().filter(s -> s.areaId().equals(areaId)).findFirst();
        if (existing.isPresent())
        {
            Subscription found = existing.get();
            if (found.minLevel() == level)
            {
                return new Result(found, false);
            }
            Subscription updated = new Subscription(found.id(), found.userId(), found.areaId(), level, found.createdAt());
            return new Result(subscriptions.save(updated), false);
        }
        if (current.size() >= props.alerts().maxSubscriptionsPerUser())
        {
            throw new BusinessRuleException("You can follow at most " + props.alerts().maxSubscriptionsPerUser() + " areas.");
        }
        Subscription created = new Subscription(UUID.randomUUID().toString(), user.id(), areaId, level, clock.instant());
        return new Result(subscriptions.save(created), true);
    }

    /**
     * Lists the subscriptions of one user, oldest first.
     *
     * @param user the user whose subscriptions are listed
     * @return the user's subscriptions, possibly empty
     */


    public List<Subscription> mine
    (
        User user
    ) {
        return subscriptions.findAll().stream()
                .filter(s -> s.userId().equals(user.id()))
                .sorted(Comparator.comparing(Subscription::createdAt))
                .toList();
    }

    /**
     * Stops following an area. Another user's subscription is reported as not found, so the id of someone
     * else's subscription is never revealed.
     *
     * @param user the user asking to unsubscribe
     * @param id   id of the subscription to delete
     * @throws NotFoundException when the subscription does not exist or belongs to another user
     */


    public void unsubscribe
    (
        User user,
        String id
    ) {
        Subscription found = subscriptions.findById(id)
                .filter(s -> s.userId().equals(user.id()))
                .orElseThrow(() -> new NotFoundException(UNKNOWN_SUBSCRIPTION));
        subscriptions.delete(found.id());
    }


    /**
     * Outcome of a subscribe call.
     *
     * @param subscription the subscription now stored for the user and area
     * @param created      true when this call created it, false when it already existed
     * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
     */


    public record Result
    (
        Subscription subscription,
        boolean created
    ) {


    }
}