package com.floodwatch.service;

import com.floodwatch.model.Shelter;
import com.floodwatch.repository.ShelterRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ShelterService {

    private final ShelterRepository shelters;

    public ShelterService(ShelterRepository shelters) {
        this.shelters = shelters;
    }

    public List<Shelter> sheltersFor(String areaId) {
        return shelters.findAll().stream()
                .filter(s -> areaId == null || areaId.isBlank() || s.areaId().equals(areaId))
                .toList();
    }
}
