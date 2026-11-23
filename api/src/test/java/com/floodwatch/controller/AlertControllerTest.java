/*
 * File:    AlertControllerTest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: HTTP tests for the resident alert list and the administrator's demo alert, including visibility rules.
 */


package com.floodwatch.controller;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.floodwatch.model.Alert;
import com.floodwatch.model.RiskLevel;
import com.floodwatch.model.Subscription;
import com.floodwatch.repository.AlertRepository;
import com.floodwatch.repository.AlertRuleRepository;
import com.floodwatch.repository.SubscriptionRepository;
import com.floodwatch.service.risk.RiskAssessmentService;
import com.floodwatch.support.AlertTestState;
import com.floodwatch.support.TestLogins;
import com.floodwatch.support.TestStorageConfig;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


/**
 * Calls /api/alerts/mine and /api/admin/alerts/demo through MockMvc. Residents must only see alerts for areas
 * they follow, at or above their minimum level.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestStorageConfig.class)
class AlertControllerTest {


    private static final String MINE = "/api/alerts/mine";
    private static final String DEMO = "/api/admin/alerts/demo";
    private static final String DEMO_PREFIX = "DEMO – not a real warning.";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AlertRepository alerts;

    @Autowired
    private SubscriptionRepository subscriptions;

    @Autowired
    private AlertRuleRepository rules;

    @MockitoBean
    private RiskAssessmentService risk;

    /**
     * Restores the seeded data before each test.
     */


    @BeforeEach
    void resetData() {
        AlertTestState.reset(subscriptions, alerts, rules);
    }

    /**
     * A resident with no subscriptions gets an empty list, even when alerts exist.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void residentWithNoSubscriptionsGetsAnEmptyList() throws Exception {
        raise("mingora", RiskLevel.HIGH, Instant.now());
        mvc.perform(get(MINE).header(HttpHeaders.AUTHORIZATION, bearer("resident2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    /**
     * Alerts for an area the resident does not follow never appear, for either resident.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void alertsForUnsubscribedAreasAreNeverShown() throws Exception {
        follow("user-resident1", "mingora", RiskLevel.MEDIUM);
        raise("mingora", RiskLevel.HIGH, Instant.now());
        raise("nowshera", RiskLevel.HIGH, Instant.now());
        mvc.perform(get(MINE).header(HttpHeaders.AUTHORIZATION, bearer("resident1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].areaId").value("mingora"));
        mvc.perform(get(MINE).header(HttpHeaders.AUTHORIZATION, bearer("resident2")))
                .andExpect(jsonPath("$.length()").value(0));
    }

    /**
     * An alert below the subscription's minimum level is hidden; one at or above it is shown.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void alertsBelowTheSubscribedLevelAreHidden() throws Exception {
        follow("user-resident2", "charsadda", RiskLevel.HIGH);
        raise("charsadda", RiskLevel.MEDIUM, Instant.now());
        raise("charsadda", RiskLevel.HIGH, Instant.now());
        mvc.perform(get(MINE).header(HttpHeaders.AUTHORIZATION, bearer("resident2")))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].level").value("HIGH"));
    }

    /**
     * Without since the list covers the last seven days: an alert eight days old is hidden, one a day old is shown.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void withoutSinceTheLastSevenDaysAreReturned() throws Exception {
        follow("user-resident1", "mingora", RiskLevel.MEDIUM);
        raise("mingora", RiskLevel.HIGH, Instant.now().minus(Duration.ofDays(8)));
        raise("mingora", RiskLevel.HIGH, Instant.now().minus(Duration.ofDays(1)));
        mvc.perform(get(MINE).header(HttpHeaders.AUTHORIZATION, bearer("resident1")))
                .andExpect(jsonPath("$.length()").value(1));
    }

    /**
     * The since filter is strict: an alert created exactly at since is excluded, one a second earlier is included.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void sinceFilterIsStrictlyAfter() throws Exception {
        follow("user-resident1", "mingora", RiskLevel.MEDIUM);
        Instant created = Instant.now().minus(Duration.ofHours(1)).truncatedTo(ChronoUnit.SECONDS);
        raise("mingora", RiskLevel.HIGH, created);
        mvc.perform(get(MINE).param("since", created.toString()).header(HttpHeaders.AUTHORIZATION, bearer("resident1")))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get(MINE).param("since", created.minusSeconds(1).toString()).header(HttpHeaders.AUTHORIZATION, bearer("resident1")))
                .andExpect(jsonPath("$.length()").value(1));
    }

    /**
     * A since in the future gives an empty list, and a since older than the retention period is accepted.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void futureSinceIsEmptyAndVeryOldSinceIsAccepted() throws Exception {
        follow("user-resident1", "mingora", RiskLevel.MEDIUM);
        raise("mingora", RiskLevel.HIGH, Instant.now());
        String future = Instant.now().plus(Duration.ofDays(1)).toString();
        mvc.perform(get(MINE).param("since", future).header(HttpHeaders.AUTHORIZATION, bearer("resident1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get(MINE).param("since", "2000-01-01T00:00:00Z").header(HttpHeaders.AUTHORIZATION, bearer("resident1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    /**
     * A since that is not an ISO instant is a bad request.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void invalidSinceIsBadRequest() throws Exception {
        mvc.perform(get(MINE).param("since", "yesterday").header(HttpHeaders.AUTHORIZATION, bearer("resident1")))
                .andExpect(status().isBadRequest());
    }

    /**
     * At most 50 alerts are returned, newest first.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void listIsNewestFirstAndLimitedToFifty() throws Exception {
        follow("user-resident1", "mingora", RiskLevel.MEDIUM);
        Instant now = Instant.now();
        for (int i = 0; i < 55; i++)
        {
            alerts.save(new Alert("alert-" + i, "mingora", RiskLevel.HIGH, "message", "reason", now.minus(Duration.ofMinutes(i)), false));
        }
        mvc.perform(get(MINE).header(HttpHeaders.AUTHORIZATION, bearer("resident1")))
                .andExpect(jsonPath("$.length()").value(50))
                .andExpect(jsonPath("$[0].id").value("alert-0"))
                .andExpect(jsonPath("$[49].id").value("alert-49"));
    }

    /**
     * An admin's demo alert is created with status 201, is flagged demo, is labelled, and reaches a subscribed resident.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void demoAlertIsLabelledAndDelivered() throws Exception {
        follow("user-resident1", "mingora", RiskLevel.MEDIUM);
        mvc.perform(post(DEMO).header(HttpHeaders.AUTHORIZATION, bearer("admin1"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"areaId\":\"mingora\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.demo").value(true))
                .andExpect(jsonPath("$.level").value("HIGH"))
                .andExpect(jsonPath("$.message").value(startsWith(DEMO_PREFIX)));
        mvc.perform(get(MINE).header(HttpHeaders.AUTHORIZATION, bearer("resident1")))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].demo").value(true));
    }

    /**
     * A second demo for the same area within a minute is refused with 429.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void secondDemoWithinAMinuteIsTooManyRequests() throws Exception {
        String token = bearer("admin1");
        mvc.perform(post(DEMO).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"areaId\":\"mingora\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post(DEMO).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"areaId\":\"mingora\"}"))
                .andExpect(status().isTooManyRequests());
    }

    /**
     * A demo for an unknown area is not found, an unknown level is a bad request, and residents may not send demos.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void demoErrorsAndRolesAreEnforced() throws Exception {
        mvc.perform(post(DEMO).header(HttpHeaders.AUTHORIZATION, bearer("admin1"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"areaId\":\"nowhere\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post(DEMO).header(HttpHeaders.AUTHORIZATION, bearer("admin1"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"areaId\":\"charsadda\",\"level\":\"SEVERE\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(DEMO).header(HttpHeaders.AUTHORIZATION, bearer("resident1"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"areaId\":\"mingora\"}"))
                .andExpect(status().isForbidden());
    }

    /**
     * Saves an alert straight into the inbox with a chosen creation time.
     *
     * @param areaId    the area the alert is about
     * @param level     the risk level
     * @param createdAt when the alert was created
     */


    private void raise
    (
        String areaId,
        RiskLevel level,
        Instant createdAt
    ) {
        alerts.save(new Alert(UUID.randomUUID().toString(), areaId, level, "message", "reason", createdAt, false));
    }

    /**
     * Saves a subscription straight into storage, so a test can set up a follow without calling the API.
     *
     * @param userId the user who follows the area
     * @param areaId the followed area
     * @param level  the minimum level
     */


    private void follow
    (
        String userId, String areaId,
        RiskLevel level
    ) {
        subscriptions.save(new Subscription(UUID.randomUUID().toString(), userId, areaId, level, Instant.now()));
    }

    /**
     * Logs in as a demo account and returns the Authorization header value.
     *
     * @param username the demo account name; the password is the name followed by "-demo"
     * @return the bearer header value
     * @throws Exception when the login fails
     */


    private String bearer
    (
        String username
    ) throws Exception {
        return "Bearer " + TestLogins.tokenFor(mvc, username, username + "-demo");
    }
}