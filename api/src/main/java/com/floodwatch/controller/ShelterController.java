package com.floodwatch.controller;

import com.floodwatch.model.Shelter;
import com.floodwatch.service.ShelterService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Feature 4: Emergency Info & Shelter Map (UC-04, SD-04). */
@RestController
@RequestMapping("/api/shelters")
public class ShelterController {

    private final ShelterService shelters;

    public ShelterController(ShelterService shelters) {
        this.shelters = shelters;
    }

    @GetMapping
    public List<Shelter> list(@RequestParam(required = false) String areaId) {
        return shelters.sheltersFor(areaId);
    }
}
