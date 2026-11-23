/*
 * File:    IncidentControllerTest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: HTTP-level tests for incident and team endpoints: roles, validation, the status lifecycle and errors.
 */


package com.floodwatch.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.floodwatch.support.IncidentTestConfig;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;


/**
 * Calls the incident and team endpoints over HTTP with the seeded accounts and in-memory data.
 * Each test creates the incidents it needs, because the storage is shared by the tests in this class.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IncidentTestConfig.class)
class IncidentControllerTest {


    private static final String LOGIN = "/api/auth/login";
    private static final String INCIDENTS = "/api/incidents";
    private static final String TEAMS = "/api/teams";
    private static final String VALID_BODY = "{\"title\":\"Road under water\",\"type\":\"FLOODED_ROAD\","
            + "\"latitude\":34.77,\"longitude\":72.36,\"description\":\"Knee deep\"}";
    private static final String UNKNOWN_ID = "inc-does-not-exist";
    private static final String STALE_MESSAGE = "This incident was changed by someone else. Reload and try again.";
    private static final String SAME_TEAM_MESSAGE = "Incident is already assigned to this team.";

    @Autowired
    private MockMvc mvc;

    /**
     * Logs in with a seeded account and returns its token.
     *
     * @param username the account name
     * @param password the account password
     * @return the login token
     * @throws Exception when the login request cannot be performed
     */


    private String tokenFor
    (
        String username, String password
    ) throws Exception {
        MvcResult result = mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    /**
     * Returns a token for the responder account.
     *
     * @return the responder token
     * @throws Exception when the login request cannot be performed
     */


    private String responder() throws Exception {
        return tokenFor("responder1", "responder1-demo");
    }

    /**
     * Returns a token for the admin account.
     *
     * @return the admin token
     * @throws Exception when the login request cannot be performed
     */


    private String admin() throws Exception {
        return tokenFor("admin1", "admin1-demo");
    }

    /**
     * Creates an incident as a responder and returns its id.
     *
     * @return the new incident id
     * @throws Exception when the request cannot be performed
     */


    private String createIncident() throws Exception {
        MvcResult result = mvc.perform(post(INCIDENTS).header(HttpHeaders.AUTHORIZATION, "Bearer " + responder())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    /**
     * A resident gets 403 on every incident endpoint and on the teams endpoint.
     *
     * @throws Exception when a request cannot be performed
     */


    @Test
    void residentIsForbiddenOnEveryIncidentEndpoint() throws Exception {
        String resident = tokenFor("resident1", "resident1-demo");
        String header = "Bearer " + resident;
        mvc.perform(post(INCIDENTS).header(HttpHeaders.AUTHORIZATION, header)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
        mvc.perform(get(INCIDENTS).header(HttpHeaders.AUTHORIZATION, header))
                .andExpect(status().isForbidden());
        mvc.perform(get(INCIDENTS + "/" + UNKNOWN_ID).header(HttpHeaders.AUTHORIZATION, header))
                .andExpect(status().isForbidden());
        mvc.perform(patch(INCIDENTS + "/" + UNKNOWN_ID + "/status").header(HttpHeaders.AUTHORIZATION, header)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESOLVED\",\"version\":1}"))
                .andExpect(status().isForbidden());
        mvc.perform(get(TEAMS).header(HttpHeaders.AUTHORIZATION, header))
                .andExpect(status().isForbidden());
    }

    /**
     * Calls without a login token get 401.
     *
     * @throws Exception when a request cannot be performed
     */


    @Test
    void anonymousCallersGet401() throws Exception {
        mvc.perform(post(INCIDENTS).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(INCIDENTS))
                .andExpect(status().isUnauthorized());
    }

    /**
     * A coordinator can read incidents and teams but cannot change a status.
     *
     * @throws Exception when a request cannot be performed
     */


    @Test
    void coordinatorCanReadButNotChangeStatus() throws Exception {
        String id = createIncident();
        String coordinator = "Bearer " + tokenFor("coordinator1", "coordinator1-demo");
        mvc.perform(get(INCIDENTS + "/" + id).header(HttpHeaders.AUTHORIZATION, coordinator))
                .andExpect(status().isOk());
        mvc.perform(get(TEAMS).header(HttpHeaders.AUTHORIZATION, coordinator))
                .andExpect(status().isOk());
        mvc.perform(patch(INCIDENTS + "/" + id + "/status").header(HttpHeaders.AUTHORIZATION, coordinator)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ASSIGNED\",\"teamId\":\"team-01\",\"version\":1}"))
                .andExpect(status().isForbidden());
    }

    /**
     * A valid report returns 201 with status OPEN, version 1, the reporter and one history entry.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void validReportReturns201WithOpenStatusAndOneHistoryEntry() throws Exception {
        mvc.perform(post(INCIDENTS).header(HttpHeaders.AUTHORIZATION, "Bearer " + responder())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.startsWith("inc-")))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.createdBy").value("user-responder1"))
                .andExpect(jsonPath("$.history.length()").value(1))
                .andExpect(jsonPath("$.history[0].to").value("OPEN"));
    }

    /**
     * Surrounding spaces in the title are removed before the length check.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void titleIsTrimmedBeforeSaving() throws Exception {
        mvc.perform(post(INCIDENTS).header(HttpHeaders.AUTHORIZATION, "Bearer " + responder())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   Trimmed road   \",\"type\":\"OTHER\",\"latitude\":34.0,\"longitude\":72.0}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Trimmed road"));
    }

    /**
     * Bad report bodies get 400 and the message names the bad field.
     *
     * @throws Exception when a request cannot be performed
     */


    @Test
    void invalidReportFieldsGive400NamingTheField() throws Exception {
        String header = "Bearer " + responder();
        String longDescription = "{\"title\":\"Valid title\",\"type\":\"OTHER\",\"latitude\":34.0,\"longitude\":72.0,\"description\":\""
                + "d".repeat(2001) + "\"}";
        mvc.perform(post(INCIDENTS).header(HttpHeaders.AUTHORIZATION, header)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY.replace("Road under water", "ab")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Title must be between 3 and 120")));
        mvc.perform(post(INCIDENTS).header(HttpHeaders.AUTHORIZATION, header)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY.replace("Road under water", "t".repeat(121))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value(containsString("title")));
        mvc.perform(post(INCIDENTS).header(HttpHeaders.AUTHORIZATION, header)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY.replace("Road under water", "   ")))
                .andExpect(status().isBadRequest());
        mvc.perform(post(INCIDENTS).header(HttpHeaders.AUTHORIZATION, header)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY.replace("FLOODED_ROAD", "FLOOD")))
                .andExpect(status().isBadRequest());
        mvc.perform(post(INCIDENTS).header(HttpHeaders.AUTHORIZATION, header)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY.replace("34.77", "10.0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value(containsString("latitude")));
        mvc.perform(post(INCIDENTS).header(HttpHeaders.AUTHORIZATION, header)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY.replace("72.36", "100.0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value(containsString("longitude")));
        mvc.perform(post(INCIDENTS).header(HttpHeaders.AUTHORIZATION, header)
                        .contentType(MediaType.APPLICATION_JSON).content(longDescription))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value(containsString("description")));
        mvc.perform(post(INCIDENTS).header(HttpHeaders.AUTHORIZATION, header)
                        .contentType(MediaType.APPLICATION_JSON).content("not json"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Unknown incident ids give 404.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void unknownIncidentIsNotFound() throws Exception {
        mvc.perform(get(INCIDENTS + "/" + UNKNOWN_ID).header(HttpHeaders.AUTHORIZATION, "Bearer " + responder()))
                .andExpect(status().isNotFound());
    }

    /**
     * The list filter accepts a real status and refuses an unknown one with 400.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void listFilterAcceptsStatusAndRejectsUnknownStatus() throws Exception {
        String id = createIncident();
        String header = "Bearer " + responder();
        mvc.perform(get(INCIDENTS + "?status=OPEN").header(HttpHeaders.AUTHORIZATION, header))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(get(INCIDENTS + "?status=BOGUS").header(HttpHeaders.AUTHORIZATION, header))
                .andExpect(status().isBadRequest());
    }

    /**
     * A PATCH without a version, or assigning without a team, gives 400. An unknown team gives 404.
     *
     * @throws Exception when a request cannot be performed
     */


    @Test
    void patchValidationGives400And404() throws Exception {
        String id = createIncident();
        String header = "Bearer " + responder();
        mvc.perform(patch(INCIDENTS + "/" + id + "/status").header(HttpHeaders.AUTHORIZATION, header)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ASSIGNED\",\"teamId\":\"team-01\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch(INCIDENTS + "/" + id + "/status").header(HttpHeaders.AUTHORIZATION, header)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ASSIGNED\",\"version\":1}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch(INCIDENTS + "/" + id + "/status").header(HttpHeaders.AUTHORIZATION, header)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ASSIGNED\",\"teamId\":\"team-99\",\"version\":1}"))
                .andExpect(status().isNotFound());
        mvc.perform(patch(INCIDENTS + "/" + UNKNOWN_ID + "/status").header(HttpHeaders.AUTHORIZATION, header)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESOLVED\",\"version\":1}"))
                .andExpect(status().isNotFound());
    }

    /**
     * A full lifecycle: assign, refuse the same team, reassign, resolve, then refuse every move after RESOLVED.
     * The history keeps every change in order.
     *
     * @throws Exception when a request cannot be performed
     */


    @Test
    void fullLifecycleKeepsOrderedHistoryAndRefusesIllegalMoves() throws Exception {
        String id = createIncident();
        String header = "Bearer " + responder();
        String path = INCIDENTS + "/" + id + "/status";
        mvc.perform(patch(path).header(HttpHeaders.AUTHORIZATION, header)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ASSIGNED\",\"teamId\":\"team-01\",\"version\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.assignedTeamId").value("team-01"));
        mvc.perform(patch(path).header(HttpHeaders.AUTHORIZATION, header)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ASSIGNED\",\"teamId\":\"team-01\",\"version\":2}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(SAME_TEAM_MESSAGE));
        mvc.perform(patch(path).header(HttpHeaders.AUTHORIZATION, header)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ASSIGNED\",\"teamId\":\"team-02\",\"version\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(3));
        mvc.perform(patch(path).header(HttpHeaders.AUTHORIZATION, header)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESOLVED\",\"version\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.version").value(4))
                .andExpect(jsonPath("$.history.length()").value(4))
                .andExpect(jsonPath("$.history[1].to").value("ASSIGNED"))
                .andExpect(jsonPath("$.history[2].teamId").value("team-02"))
                .andExpect(jsonPath("$.history[3].to").value("RESOLVED"));
        mvc.perform(patch(path).header(HttpHeaders.AUTHORIZATION, header)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"OPEN\",\"version\":4}"))
                .andExpect(status().isConflict());
        mvc.perform(patch(path).header(HttpHeaders.AUTHORIZATION, header)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ASSIGNED\",\"teamId\":\"team-01\",\"version\":4}"))
                .andExpect(status().isConflict());
    }

    /**
     * A stale version gives 409 with the standard message and the incident keeps its newer state.
     *
     * @throws Exception when a request cannot be performed
     */


    @Test
    void staleVersionGives409() throws Exception {
        String id = createIncident();
        String header = "Bearer " + responder();
        String path = INCIDENTS + "/" + id + "/status";
        mvc.perform(patch(path).header(HttpHeaders.AUTHORIZATION, header)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ASSIGNED\",\"teamId\":\"team-01\",\"version\":1}"))
                .andExpect(status().isOk());
        mvc.perform(patch(path).header(HttpHeaders.AUTHORIZATION, header)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ASSIGNED\",\"teamId\":\"team-02\",\"version\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(STALE_MESSAGE));
    }

    /**
     * A responder cannot close an OPEN incident as a false alarm (403), but an admin can (200).
     *
     * @throws Exception when a request cannot be performed
     */


    @Test
    void responderGets403ForFalseAlarmButAdminCanClose() throws Exception {
        String id = createIncident();
        mvc.perform(patch(INCIDENTS + "/" + id + "/status").header(HttpHeaders.AUTHORIZATION, "Bearer " + responder())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESOLVED\",\"version\":1}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch(INCIDENTS + "/" + id + "/status").header(HttpHeaders.AUTHORIZATION, "Bearer " + admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESOLVED\",\"version\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    /**
     * The team list gives the three seeded teams to responders.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void teamsListsTheSeededTeams() throws Exception {
        mvc.perform(get(TEAMS).header(HttpHeaders.AUTHORIZATION, "Bearer " + responder()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value("team-01"))
                .andExpect(jsonPath("$[0].name").value("Rescue Team Swat"));
    }
}