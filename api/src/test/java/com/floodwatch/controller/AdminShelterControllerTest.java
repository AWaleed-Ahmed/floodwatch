/*
 * File:    AdminShelterControllerTest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: HTTP tests for the admin shelter endpoints: validation, status codes, duplicates and roles.
 */


package com.floodwatch.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.floodwatch.repository.AlertRepository;
import com.floodwatch.repository.AlertRuleRepository;
import com.floodwatch.repository.AreaRepository;
import com.floodwatch.repository.ShelterRepository;
import com.floodwatch.repository.SubscriptionRepository;
import com.floodwatch.service.risk.RiskAssessmentService;
import com.floodwatch.support.AlertTestState;
import com.floodwatch.support.SeedReset;
import com.floodwatch.support.TestLogins;
import com.floodwatch.support.TestStorageConfig;
import com.jayway.jsonpath.JsonPath;
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
 * Calls the admin shelter endpoints through MockMvc. Writes go to an in-memory copy of the test data.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestStorageConfig.class)
class AdminShelterControllerTest {


    private static final String SHELTERS = "/api/admin/shelters";
    private static final String VALID = "{\"areaId\":\"mingora\",\"name\":\"  Swat Girls School  \",\"latitude\":34.77,"
            + "\"longitude\":72.36,\"capacity\":200,\"status\":\"OPEN\",\"contact\":\"0300-0000000\"}";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ShelterRepository shelters;

    @Autowired
    private AreaRepository areas;

    @Autowired
    private AlertRepository alerts;

    @Autowired
    private AlertRuleRepository rules;

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
        SeedReset.restore(areas, shelters);
    }

    /**
     * A valid shelter is created with status 201, a sh- id, a trimmed name, and sample defaulting to true.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void validShelterIsCreatedWithDefaults() throws Exception {
        mvc.perform(post(SHELTERS).header(HttpHeaders.AUTHORIZATION, bearer("admin1"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.startsWith("sh-")))
                .andExpect(jsonPath("$.name").value("Swat Girls School"))
                .andExpect(jsonPath("$.sample").value(true))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    /**
     * Each invalid field gives 400: short or long name, coordinates outside Pakistan, capacity out of range or missing,
     * unknown status, long contact, missing status and blank area.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void invalidFieldsAreBadRequests() throws Exception {
        String longContact = "x".repeat(61);
        String[] bodies = {
            "{\"areaId\":\"mingora\",\"name\":\"ab\",\"latitude\":34.77,\"longitude\":72.36,\"capacity\":1,\"status\":\"OPEN\"}",
            "{\"areaId\":\"mingora\",\"name\":\"Good name\",\"latitude\":10.0,\"longitude\":72.36,\"capacity\":1,\"status\":\"OPEN\"}",
            "{\"areaId\":\"mingora\",\"name\":\"Good name\",\"latitude\":34.77,\"longitude\":90.0,\"capacity\":1,\"status\":\"OPEN\"}",
            "{\"areaId\":\"mingora\",\"name\":\"Good name\",\"latitude\":34.77,\"longitude\":72.36,\"capacity\":100001,\"status\":\"OPEN\"}",
            "{\"areaId\":\"mingora\",\"name\":\"Good name\",\"latitude\":34.77,\"longitude\":72.36,\"status\":\"OPEN\"}",
            "{\"areaId\":\"mingora\",\"name\":\"Good name\",\"latitude\":34.77,\"longitude\":72.36,\"capacity\":1,\"status\":\"SEVERE\"}",
            "{\"areaId\":\"mingora\",\"name\":\"Good name\",\"latitude\":34.77,\"longitude\":72.36,\"capacity\":1}",
            "{\"areaId\":\"mingora\",\"name\":\"Good name\",\"latitude\":34.77,\"longitude\":72.36,\"capacity\":1,\"status\":\"OPEN\",\"contact\":\"" + longContact + "\"}",
            "{\"areaId\":\" \",\"name\":\"Good name\",\"latitude\":34.77,\"longitude\":72.36,\"capacity\":1,\"status\":\"OPEN\"}"
        };
        String token = bearer("admin1");
        for (String body : bodies)
        {
            mvc.perform(post(SHELTERS).header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    /**
     * An area that does not exist gives 404.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void unknownAreaIsNotFound() throws Exception {
        mvc.perform(post(SHELTERS).header(HttpHeaders.AUTHORIZATION, bearer("admin1"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID.replace("mingora", "nowhere")))
                .andExpect(status().isNotFound());
    }

    /**
     * A duplicate name in the same area gives 409 whatever the letter case; the same name in another area is allowed.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void duplicateNameInTheSameAreaIsConflictIgnoringCase() throws Exception {
        String token = bearer("admin1");
        mvc.perform(post(SHELTERS).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isCreated());
        mvc.perform(post(SHELTERS).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID.replace("Swat Girls School", "swat GIRLS school")))
                .andExpect(status().isConflict());
        mvc.perform(post(SHELTERS).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID.replace("mingora", "charsadda")))
                .andExpect(status().isCreated());
    }

    /**
     * A PUT keeps the shelter's own name without a conflict, and changes the other fields.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void putUpdatesAShelterAndIgnoresItsOwnName() throws Exception {
        String token = bearer("admin1");
        String id = createShelter(token);
        mvc.perform(put(SHELTERS + "/" + id).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID.replace("200", "350")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.capacity").value(350));
    }

    /**
     * A PUT or DELETE on an unknown id gives 404.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void unknownShelterIdIsNotFoundForPutAndDelete() throws Exception {
        String token = bearer("admin1");
        mvc.perform(put(SHELTERS + "/sh-missing").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isNotFound());
        mvc.perform(delete(SHELTERS + "/sh-missing").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound());
    }

    /**
     * A deleted shelter answers 204 once, and is not found afterwards.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void deleteRemovesTheShelter() throws Exception {
        String token = bearer("admin1");
        String id = createShelter(token);
        mvc.perform(delete(SHELTERS + "/" + id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());
        mvc.perform(delete(SHELTERS + "/" + id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound());
    }

    /**
     * Residents may not create shelters, and the public list still works without a login.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void residentIsForbiddenAndPublicListStaysOpen() throws Exception {
        mvc.perform(post(SHELTERS).header(HttpHeaders.AUTHORIZATION, bearer("resident1"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/shelters"))
                .andExpect(status().isOk());
    }

    /**
     * Creates a valid shelter through the API and returns its id.
     *
     * @param token the Authorization header value
     * @return the new shelter id
     * @throws Exception when the request cannot be performed
     */


    private String createShelter
    (
        String token
    ) throws Exception {
        String json = mvc.perform(post(SHELTERS).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
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