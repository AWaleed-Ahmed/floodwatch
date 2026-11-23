package com.floodwatch.controller;

import com.floodwatch.model.Area;
import com.floodwatch.repository.AreaRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/areas")
public class AreaController {

    private final AreaRepository areas;

    public AreaController(AreaRepository areas) {
        this.areas = areas;
    }

    @GetMapping
    public List<Area> list() {
        return areas.findAll();
    }
}
