/*
 * File:    SubscriptionServiceTest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: Checks the subscription rules: duplicates, the per-user limit, unknown areas and ownership on delete.
 */


package com.floodwatch.service.alerts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.floodwatch.model.Area;
import com.floodwatch.model.Role;
import com.floodwatch.model.RiskLevel;
import com.floodwatch.model.User;
import com.floodwatch.repository.AreaRepository;
import com.floodwatch.repository.SubscriptionRepository;
import com.floodwatch.service.BusinessRuleException;
import com.floodwatch.service.NotFoundException;
import com.floodwatch.support.InMemoryJsonStorage;
import com.floodwatch.support.MutableClock;
import com.floodwatch.support.TestProperties;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;


/**
 * Unit tests for subscriptions. Twelve areas are seeded so the ten-area limit can be crossed.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


class SubscriptionServiceTest {


    private static final int LIMIT = 10;
    private static final User ALI = new User("user-ali", "ali", "hash", "Ali", Role.RESIDENT, null);
    private static final User BILAL = new User("user-bilal", "bilal", "hash", "Bilal", Role.RESIDENT, null);

    private AreaRepository areas;
    private SubscriptionService service;

    /**
     * Creates fresh storage with twelve areas before each test.
     */


    @BeforeEach
    void setUp() {
        InMemoryJsonStorage storage = new InMemoryJsonStorage();
        JsonMapper json = JsonMapper.builder().build();
        areas = new AreaRepository(storage, json);
        for (int i = 1; i <= 12; i++)
        {
            areas.save(new Area("area-" + i, "Area " + i, 34.0, 71.0, "Kabul River"));
        }
        SubscriptionRepository subscriptions = new SubscriptionRepository(storage, json);
        service = new SubscriptionService(subscriptions, areas, TestProperties.alerts(false, 6, 30, LIMIT), new MutableClock(Instant.parse("2026-11-23T06:00:00Z")));
    }

    /**
     * Following an area without a level stores MEDIUM; following it again keeps one record and updates the level.
     */


    @Test
    void resubscribingKeepsOneRecordAndUpdatesTheLevel() {
        SubscriptionService.Result first = service.subscribe(ALI, "area-1", null);
        assertThat(first.created()).isTrue();
        assertThat(first.subscription().minLevel()).isEqualTo(RiskLevel.MEDIUM);
        SubscriptionService.Result again = service.subscribe(ALI, "area-1", RiskLevel.HIGH);
        assertThat(again.created()).isFalse();
        assertThat(again.subscription().id()).isEqualTo(first.subscription().id());
        assertThat(again.subscription().minLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(service.mine(ALI)).hasSize(1);
    }

    /**
     * The eleventh distinct area is refused with a business-rule error, while updating an area already followed still works.
     */


    @Test
    void eleventhAreaIsRefusedButUpdatingAnExistingOneIsNot() {
        for (int i = 1; i <= LIMIT; i++)
        {
            service.subscribe(ALI, "area-" + i, null);
        }
        assertThatThrownBy(() -> service.subscribe(ALI, "area-11", null)).isInstanceOf(BusinessRuleException.class);
        assertThat(service.subscribe(ALI, "area-1", RiskLevel.HIGH).created()).isFalse();
        assertThat(service.mine(ALI)).hasSize(LIMIT);
    }

    /**
     * An area that does not exist cannot be followed.
     */


    @Test
    void unknownAreaIsNotFound() {
        assertThatThrownBy(() -> service.subscribe(ALI, "nowhere", null)).isInstanceOf(NotFoundException.class);
    }

    /**
     * A user only sees their own subscriptions.
     */


    @Test
    void listsOnlyTheCallersSubscriptions() {
        service.subscribe(ALI, "area-1", null);
        service.subscribe(BILAL, "area-2", null);
        assertThat(service.mine(ALI)).extracting(s -> s.areaId()).containsExactly("area-1");
        assertThat(service.mine(BILAL)).extracting(s -> s.areaId()).containsExactly("area-2");
    }

    /**
     * Another user's subscription is reported as not found and is not removed.
     */


    @Test
    void deletingSomeoneElsesSubscriptionIsNotFound() {
        String id = service.subscribe(ALI, "area-1", null).subscription().id();
        assertThatThrownBy(() -> service.unsubscribe(BILAL, id)).isInstanceOf(NotFoundException.class);
        assertThat(service.mine(ALI)).hasSize(1);
    }
}