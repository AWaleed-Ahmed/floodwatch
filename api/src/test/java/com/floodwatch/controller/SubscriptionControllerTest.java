/*
 * File:    SubscriptionControllerTest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: HTTP tests for following and unfollowing areas, run against the real application with in-memory data.
 */


package com.floodwatch.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.floodwatch.repository.AlertRepository;
import com.floodwatch.repository.AlertRuleRepository;
import com.floodwatch.repository.SubscriptionRepository;
import com.floodwatch.service.risk.RiskAssessmentService;
import com.floodwatch.support.AlertTestState;
import com.floodwatch.support.TestLogins;
import com.floodwatch.support.TestStorageConfig;
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
 * Calls the subscription endpoints through MockMvc. Writes go to an in-memory copy of the test data.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestStorageConfig.class)
class SubscriptionControllerTest {


    private static final String SUBSCRIPTIONS = "/api/subscriptions";
    private static final String MINE = "/api/subscriptions/mine";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SubscriptionRepository subscriptions;

    @Autowired
    private AlertRepository alerts;

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
     * A new subscription is created with status 201, and without a level it is MEDIUM.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void newSubscriptionIsCreatedWithDefaultMediumLevel() throws Exception {
        mvc.perform(post(SUBSCRIPTIONS).header(HttpHeaders.AUTHORIZATION, bearer("resident1"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"areaId\":\"mingora\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.areaId").value("mingora"))
                .andExpect(jsonPath("$.minLevel").value("MEDIUM"))
                .andExpect(jsonPath("$.userId").value("user-resident1"));
    }

    /**
     * Following the same area twice returns 200 with the existing subscription and creates no duplicate.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void followingTheSameAreaTwiceReturnsTheExistingSubscription() throws Exception {
        String token = bearer("resident1");
        mvc.perform(post(SUBSCRIPTIONS).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"areaId\":\"mingora\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post(SUBSCRIPTIONS).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"areaId\":\"mingora\",\"minLevel\":\"HIGH\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minLevel").value("HIGH"));
        mvc.perform(get(MINE).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    /**
     * An unknown area is not found; an unknown level or a missing area is a bad request.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void unknownAreaIsNotFoundAndBadInputIsBadRequest() throws Exception {
        String token = bearer("resident1");
        mvc.perform(post(SUBSCRIPTIONS).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"areaId\":\"nowhere\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post(SUBSCRIPTIONS).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"areaId\":\"mingora\",\"minLevel\":\"SEVERE\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(SUBSCRIPTIONS).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    /**
     * A resident with no subscriptions gets an empty list, not an error.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void residentWithNoSubscriptionsGetsAnEmptyList() throws Exception {
        mvc.perform(get(MINE).header(HttpHeaders.AUTHORIZATION, bearer("resident2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    /**
     * The owner can delete a subscription; it answers 204 and is gone afterwards.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void ownerCanDeleteTheirSubscription() throws Exception {
        String token = bearer("resident1");
        String id = createSubscription(token, "mingora");
        mvc.perform(delete(SUBSCRIPTIONS + "/" + id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());
        mvc.perform(get(MINE).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.length()").value(0));
    }

    /**
     * Deleting someone else's subscription or an unknown id is not found, and the other subscription survives.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void deletingAnotherUsersOrAnUnknownSubscriptionIsNotFound() throws Exception {
        String id = createSubscription(bearer("resident1"), "mingora");
        mvc.perform(delete(SUBSCRIPTIONS + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer("resident2")))
                .andExpect(status().isNotFound());
        mvc.perform(delete(SUBSCRIPTIONS + "/no-such-id").header(HttpHeaders.AUTHORIZATION, bearer("resident1")))
                .andExpect(status().isNotFound());
        mvc.perform(get(MINE).header(HttpHeaders.AUTHORIZATION, bearer("resident1")))
                .andExpect(jsonPath("$.length()").value(1));
    }

    /**
     * Without a token the call is unauthorized, and a responder is forbidden because the role is not allowed.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void anonymousIsUnauthorizedAndResponderIsForbidden() throws Exception {
        mvc.perform(get(MINE))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(MINE).header(HttpHeaders.AUTHORIZATION, bearer("responder1")))
                .andExpect(status().isForbidden());
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

    /**
     * Creates a subscription through the API and returns its id.
     *
     * @param token  the Authorization header value
     * @param areaId the area to follow
     * @return the new subscription id
     * @throws Exception when the request cannot be performed
     */


    private String createSubscription
    (
        String token, String areaId
    ) throws Exception {
        String body = "{\"areaId\":\"" + areaId + "\"}";
        String json = mvc.perform(post(SUBSCRIPTIONS).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(json, "$.id");
    }
}