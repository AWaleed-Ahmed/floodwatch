/*
 * File:    AlertRuleControllerTest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: HTTP tests for reading and changing the alert rules, for administrators only.
 */


package com.floodwatch.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 * Calls /api/admin/alert-rules through MockMvc. Writes go to an in-memory copy of the test data.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestStorageConfig.class)
class AlertRuleControllerTest {


    private static final String RULES = "/api/admin/alert-rules";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AlertRuleRepository rules;

    @Autowired
    private AlertRepository alerts;

    @Autowired
    private SubscriptionRepository subscriptions;

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
     * The seeded rules are listed in area id order.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void adminSeesEveryRuleInAreaOrder() throws Exception {
        mvc.perform(get(RULES).header(HttpHeaders.AUTHORIZATION, bearer("admin1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].areaId").value("charsadda"))
                .andExpect(jsonPath("$[0].minLevel").value("MEDIUM"))
                .andExpect(jsonPath("$[0].enabled").value(true));
    }

    /**
     * A PUT replaces the rule of an area, and the next read shows the new values.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void putReplacesTheRule() throws Exception {
        String token = bearer("admin1");
        mvc.perform(put(RULES + "/mingora").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"minLevel\":\"HIGH\",\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.areaId").value("mingora"))
                .andExpect(jsonPath("$.minLevel").value("HIGH"))
                .andExpect(jsonPath("$.enabled").value(false));
        mvc.perform(get(RULES).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$[1].minLevel").value("HIGH"))
                .andExpect(jsonPath("$[1].enabled").value(false));
    }

    /**
     * A PUT for an unknown area is not found; an invalid level or a missing enabled flag is a bad request.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void putErrorsAreReported() throws Exception {
        String token = bearer("admin1");
        mvc.perform(put(RULES + "/nowhere").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"minLevel\":\"HIGH\",\"enabled\":true}"))
                .andExpect(status().isNotFound());
        mvc.perform(put(RULES + "/mingora").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"minLevel\":\"SEVERE\",\"enabled\":true}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put(RULES + "/mingora").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"minLevel\":\"HIGH\"}"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Residents may not read or change the rules.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void residentsAreForbidden() throws Exception {
        mvc.perform(get(RULES).header(HttpHeaders.AUTHORIZATION, bearer("resident1")))
                .andExpect(status().isForbidden());
        mvc.perform(put(RULES + "/mingora").header(HttpHeaders.AUTHORIZATION, bearer("resident1"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"minLevel\":\"HIGH\",\"enabled\":true}"))
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
}