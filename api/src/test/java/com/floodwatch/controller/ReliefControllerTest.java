/*
 * File:    ReliefControllerTest.java
 * Authors: Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 * Purpose: HTTP tests for supplies, allocations and volunteers: roles, validation, status codes and phone privacy.
 */


package com.floodwatch.controller;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.floodwatch.repository.AllocationRepository;
import com.floodwatch.repository.SupplyRepository;
import com.floodwatch.service.relief.ReliefInvariants;
import com.floodwatch.service.relief.ReliefTestConfig;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
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
 * Calls the supply, allocation and volunteer endpoints over HTTP with the seeded accounts and in-memory data.
 * Each test uses its own supplies and incidents, because the storage is shared by the tests in this class.
 *
 * @author Abdullah Waleed Ahmed, Muhammad Rehan Haider, Syed Ehtisham Haider Bukhari, Faheem Ahmad Khan
 */


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ReliefTestConfig.class)
class ReliefControllerTest {


    private static final String LOGIN = "/api/auth/login";
    private static final String SUPPLIES = "/api/supplies";
    private static final String ALLOCATIONS = "/api/allocations";
    private static final String VOLUNTEERS = "/api/volunteers";
    private static final String INCIDENTS = "/api/incidents";
    private static final String PHONE_KEY = "phone";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SupplyRepository supplyRepository;

    @Autowired
    private AllocationRepository allocationRepository;

    /**
     * Logs in with a seeded account and returns the Authorization header value.
     *
     * @param username the account name
     * @param password the account password
     * @return "Bearer" followed by the login token
     * @throws Exception when the login request cannot be performed
     */


    private String bearer
    (
        String username, String password
    ) throws Exception {
        MvcResult result = mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return "Bearer " + JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    /**
     * Creates a supply with a unique name and returns its id.
     *
     * @param available the units in stock
     * @return the new supply id
     * @throws Exception when the request cannot be performed
     */


    private String createSupply
    (
        int available
    ) throws Exception {
        String body = "{\"item\":\"Item " + java.util.UUID.randomUUID() + "\",\"unit\":\"pcs\",\"available\":" + available + "}";
        MvcResult result = mvc.perform(post(SUPPLIES).header(HttpHeaders.AUTHORIZATION, bearer("coordinator1", "coordinator1-demo"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    /**
     * Reports an incident as a responder and assigns it to team-01, so it can receive supplies.
     *
     * @return the assigned incident id
     * @throws Exception when a request cannot be performed
     */


    private String assignedIncident() throws Exception {
        String responder = bearer("responder1", "responder1-demo");
        MvcResult created = mvc.perform(post(INCIDENTS).header(HttpHeaders.AUTHORIZATION, responder)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Relief case\",\"type\":\"RESCUE_NEEDED\",\"latitude\":34.7,\"longitude\":72.3}"))
                .andExpect(status().isCreated())
                .andReturn();
        String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        mvc.perform(patch(INCIDENTS + "/" + id + "/status").header(HttpHeaders.AUTHORIZATION, responder)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ASSIGNED\",\"teamId\":\"team-01\",\"version\":1}"))
                .andExpect(status().isOk());
        return id;
    }

    /**
     * Checks the stock invariants against the stored data.
     */


    private void checkInvariants() {
        ReliefInvariants.assertHold(supplyRepository, allocationRepository);
    }

    /**
     * A resident gets 403 on supplies, allocations and volunteers.
     *
     * @throws Exception when a request cannot be performed
     */


    @Test
    void residentIsForbiddenOnRelief() throws Exception {
        String resident = bearer("resident1", "resident1-demo");
        mvc.perform(get(SUPPLIES).header(HttpHeaders.AUTHORIZATION, resident)).andExpect(status().isForbidden());
        mvc.perform(get(ALLOCATIONS).header(HttpHeaders.AUTHORIZATION, resident)).andExpect(status().isForbidden());
        mvc.perform(get(VOLUNTEERS).header(HttpHeaders.AUTHORIZATION, resident)).andExpect(status().isForbidden());
    }

    /**
     * Responders get volunteers without any phone key: the word phone does not appear in the body and no
     * volunteer map contains the key.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void responderNeverSeesVolunteerPhoneNumbers() throws Exception {
        MvcResult result = mvc.perform(get(VOLUNTEERS).header(HttpHeaders.AUTHORIZATION, bearer("responder1", "responder1-demo")))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains(PHONE_KEY), "the responder response must not contain a phone key");
        List<Map<String, Object>> volunteers = JsonPath.read(body, "$");
        assertTrue(volunteers.size() >= 4);
        for (Map<String, Object> volunteer : volunteers)
        {
            assertFalse(volunteer.containsKey(PHONE_KEY));
        }
    }

    /**
     * Coordinators see the phone number of each volunteer.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void coordinatorSeesVolunteerPhoneNumbers() throws Exception {
        mvc.perform(get(VOLUNTEERS).header(HttpHeaders.AUTHORIZATION, bearer("coordinator1", "coordinator1-demo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].phone").value("+92 000 0000001"));
    }

    /**
     * A new volunteer is saved and returned with its phone number, for coordinators.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void coordinatorCanAddVolunteer() throws Exception {
        mvc.perform(post(VOLUNTEERS).header(HttpHeaders.AUTHORIZATION, bearer("coordinator1", "coordinator1-demo"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Helper\",\"phone\":\"+92 000 0000009\",\"skills\":[\"first aid\"],\"available\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.startsWith("vol-")))
                .andExpect(jsonPath("$.phone").value("+92 000 0000009"));
    }

    /**
     * Supply names are unique ignoring case and spaces: a clash gives 409.
     *
     * @throws Exception when the request cannot be performed
     */


    @Test
    void duplicateSupplyNameIsConflict() throws Exception {
        String coordinator = bearer("coordinator1", "coordinator1-demo");
        String name = "Duplicate " + java.util.UUID.randomUUID();
        mvc.perform(post(SUPPLIES).header(HttpHeaders.AUTHORIZATION, coordinator).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"item\":\"" + name + "\",\"unit\":\"pcs\",\"available\":5}"))
                .andExpect(status().isCreated());
        mvc.perform(post(SUPPLIES).header(HttpHeaders.AUTHORIZATION, coordinator).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"item\":\"" + name.toUpperCase(java.util.Locale.ROOT).replace(" ", "") + "\",\"unit\":\"pcs\",\"available\":5}"))
                .andExpect(status().isConflict());
    }

    /**
     * Quantities must be whole numbers from 1: zero, negative and fractional values give 400, and so does
     * an available count below 0.
     *
     * @throws Exception when a request cannot be performed
     */


    @Test
    void quantitiesMustBeWholeNumbersFromOne() throws Exception {
        String coordinator = bearer("coordinator1", "coordinator1-demo");
        String supplyId = createSupply(10);
        String incidentId = assignedIncident();
        for (String quantity : new String[] {"0", "-1", "1.5"})
        {
            mvc.perform(post(ALLOCATIONS).header(HttpHeaders.AUTHORIZATION, coordinator)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"supplyId\":\"" + supplyId + "\",\"incidentId\":\"" + incidentId + "\",\"quantity\":" + quantity + "}"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post(SUPPLIES).header(HttpHeaders.AUTHORIZATION, coordinator).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"item\":\"Negative " + java.util.UUID.randomUUID() + "\",\"unit\":\"pcs\",\"available\":-1}"))
                .andExpect(status().isBadRequest());
        checkInvariants();
    }

    /**
     * A full allocation flow over HTTP: allocate, refuse too much, refuse an OPEN incident, deliver, refuse a
     * second delivery, and read the allocations of the incident. Invariants hold at the end.
     *
     * @throws Exception when a request cannot be performed
     */


    @Test
    void allocationFlowOverHttpFollowsTheRules() throws Exception {
        String coordinator = bearer("coordinator1", "coordinator1-demo");
        String supplyId = createSupply(5);
        String incidentId = assignedIncident();
        MvcResult allocated = mvc.perform(post(ALLOCATIONS).header(HttpHeaders.AUTHORIZATION, coordinator)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"supplyId\":\"" + supplyId + "\",\"incidentId\":\"" + incidentId + "\",\"quantity\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RESERVED"))
                .andReturn();
        String allocationId = JsonPath.read(allocated.getResponse().getContentAsString(), "$.id");

        mvc.perform(post(ALLOCATIONS).header(HttpHeaders.AUTHORIZATION, coordinator)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"supplyId\":\"" + supplyId + "\",\"incidentId\":\"" + incidentId + "\",\"quantity\":3}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value(containsString("left.")));

        String openId = createOpenIncident();
        mvc.perform(post(ALLOCATIONS).header(HttpHeaders.AUTHORIZATION, coordinator)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"supplyId\":\"" + supplyId + "\",\"incidentId\":\"" + openId + "\",\"quantity\":1}"))
                .andExpect(status().isUnprocessableContent());

        mvc.perform(patch(ALLOCATIONS + "/" + allocationId + "/deliver").header(HttpHeaders.AUTHORIZATION, coordinator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
        mvc.perform(patch(ALLOCATIONS + "/" + allocationId + "/deliver").header(HttpHeaders.AUTHORIZATION, coordinator))
                .andExpect(status().isConflict());

        mvc.perform(get(ALLOCATIONS + "?incidentId=" + incidentId).header(HttpHeaders.AUTHORIZATION, bearer("responder1", "responder1-demo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("DELIVERED"));
        checkInvariants();
    }

    /**
     * A stale stock correction gives 409, and only an admin can delete a supply (a coordinator gets 403).
     *
     * @throws Exception when a request cannot be performed
     */


    @Test
    void staleCorrectionIsConflictAndOnlyAdminDeletes() throws Exception {
        String coordinator = bearer("coordinator1", "coordinator1-demo");
        String supplyId = createSupply(4);
        mvc.perform(patch(SUPPLIES + "/" + supplyId).header(HttpHeaders.AUTHORIZATION, coordinator)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"available\":9,\"version\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(9));
        mvc.perform(patch(SUPPLIES + "/" + supplyId).header(HttpHeaders.AUTHORIZATION, coordinator)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"available\":2,\"version\":1}"))
                .andExpect(status().isConflict());

        mvc.perform(delete(SUPPLIES + "/" + supplyId).header(HttpHeaders.AUTHORIZATION, coordinator))
                .andExpect(status().isForbidden());
        mvc.perform(delete(SUPPLIES + "/" + supplyId).header(HttpHeaders.AUTHORIZATION, bearer("admin1", "admin1-demo")))
                .andExpect(status().isNoContent());
        checkInvariants();
    }

    /**
     * Creates an incident and leaves it OPEN, for the test that checks an OPEN incident is refused.
     *
     * @return the OPEN incident id
     * @throws Exception when the request cannot be performed
     */


    private String createOpenIncident() throws Exception {
        MvcResult created = mvc.perform(post(INCIDENTS).header(HttpHeaders.AUTHORIZATION, bearer("responder1", "responder1-demo"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Open case\",\"type\":\"OTHER\",\"latitude\":34.7,\"longitude\":72.3}"))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(created.getResponse().getContentAsString(), "$.id");
    }
}