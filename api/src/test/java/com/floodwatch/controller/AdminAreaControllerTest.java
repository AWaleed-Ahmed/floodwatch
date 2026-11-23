/*
 * File:    AdminAreaControllerTest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: HTTP tests for the admin area endpoints: id rules, coordinates, the disabled rule and the in-use delete message.
 */


package com.floodwatch.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.floodwatch.model.RiskLevel;
import com.floodwatch.model.Subscription;
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
import java.time.Instant;
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
 * Calls the admin area endpoints through MockMvc. Writes go to an in-memory copy of the test data.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestStorageConfig.class)
class AdminAreaControllerTest {


    private static final String AREAS = "/api/admin/areas";
    private static final String RULES = "/api/admin/alert-rules";
    private static final String NEW_AREA = "{\"id\":\"swat-upper\",\"name\":\"Upper Swat\",\"latitude\":35.2,"
            + "\"longitude\":72.6,\"riverName\":\"Swat River\"}";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AreaRepository areas;

    @Autowired
    private ShelterRepository shelters;

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
        SeedReset.restore(areas, shelters);
    }

    /**
     * Creating an area also creates a disabled MEDIUM alert rule for it.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void createdAreaGetsADisabledMediumRule() throws Exception {
        String token = bearer("admin1");
        mvc.perform(post(AREAS).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_AREA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("swat-upper"));
        mvc.perform(get(RULES).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[3].areaId").value("swat-upper"))
                .andExpect(jsonPath("$[3].minLevel").value("MEDIUM"))
                .andExpect(jsonPath("$[3].enabled").value(false));
    }

    /**
     * Ids that are too short, too long, uppercase or with other characters give 400.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void invalidIdsAreBadRequests() throws Exception {
        String token = bearer("admin1");
        String[] ids = {"a", "Bad_Id", "UPPER", "x".repeat(41)};
        for (String id : ids)
        {
            mvc.perform(post(AREAS).header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON).content(NEW_AREA.replace("swat-upper", id)))
                    .andExpect(status().isBadRequest());
        }
    }

    /**
     * An id that already exists gives 409, and coordinates outside Pakistan give 400.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void duplicateIdIsConflictAndOutOfCountryCoordinatesAreBadRequest() throws Exception {
        String token = bearer("admin1");
        mvc.perform(post(AREAS).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_AREA.replace("swat-upper", "mingora")))
                .andExpect(status().isConflict());
        mvc.perform(post(AREAS).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_AREA.replace("35.2", "40.0")))
                .andExpect(status().isBadRequest());
    }

    /**
     * A PUT changes the area's details and leaves its alert rule as it was. An unknown area gives 404.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void putChangesTheAreaAndKeepsItsRule() throws Exception {
        String token = bearer("admin1");
        mvc.perform(put(AREAS + "/mingora").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mingora Town\",\"latitude\":34.77,\"longitude\":72.36,\"riverName\":\"Swat River\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mingora Town"));
        mvc.perform(get(RULES).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$[1].areaId").value("mingora"))
                .andExpect(jsonPath("$[1].enabled").value(true));
        mvc.perform(put(AREAS + "/nowhere").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nowhere\",\"latitude\":34.77,\"longitude\":72.36,\"riverName\":\"Swat River\"}"))
                .andExpect(status().isNotFound());
    }

    /**
     * An area with only its disabled rule is deleted with 204, and its rule goes with it.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void unusedAreaIsDeletedWithItsDisabledRule() throws Exception {
        String token = bearer("admin1");
        mvc.perform(post(AREAS).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_AREA))
                .andExpect(status().isCreated());
        mvc.perform(delete(AREAS + "/swat-upper").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());
        mvc.perform(get(RULES).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.length()").value(3));
        mvc.perform(delete(AREAS + "/nowhere").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound());
    }

    /**
     * An area still used by two shelters, one subscription and one enabled rule cannot be deleted; the message counts each kind.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void areaInUseCannotBeDeletedAndTheMessageCountsEachUse() throws Exception {
        String admin = bearer("admin1");
        mvc.perform(post(AREAS).header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_AREA))
                .andExpect(status().isCreated());
        addShelter(admin, "Upper Shelter One");
        addShelter(admin, "Upper Shelter Two");
        subscriptions.save(new Subscription(UUID.randomUUID().toString(), "user-resident1", "swat-upper", RiskLevel.MEDIUM, Instant.now()));
        mvc.perform(put(RULES + "/swat-upper").header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"minLevel\":\"MEDIUM\",\"enabled\":true}"))
                .andExpect(status().isOk());
        mvc.perform(delete(AREAS + "/swat-upper").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Area is still used by 2 shelters, 1 subscription and 1 alert rule."));
        mvc.perform(get(RULES).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(jsonPath("$.length()").value(4));
    }

    /**
     * A single shelter is described with the singular word, and residents may not change areas.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void singularUseAndResidentRoleAreChecked() throws Exception {
        String admin = bearer("admin1");
        mvc.perform(post(AREAS).header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_AREA))
                .andExpect(status().isCreated());
        addShelter(admin, "Upper Shelter One");
        mvc.perform(delete(AREAS + "/swat-upper").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Area is still used by 1 shelter."));
        mvc.perform(post(AREAS).header(HttpHeaders.AUTHORIZATION, bearer("resident1"))
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_AREA))
                .andExpect(status().isForbidden());
    }

    /**
     * Adds a shelter to the swat-upper area through the admin API.
     *
     * @param token the Authorization header value
     * @param name  the shelter name
     * @throws Exception when the request cannot be performed
     */


    private void addShelter
    (
        String token, String name
    ) throws Exception {
        String body = "{\"areaId\":\"swat-upper\",\"name\":\"" + name + "\",\"latitude\":35.2,\"longitude\":72.6,"
                + "\"capacity\":50,\"status\":\"OPEN\"}";
        mvc.perform(post("/api/admin/shelters").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
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