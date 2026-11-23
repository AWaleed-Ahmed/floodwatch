package com.floodwatch.repository;

import com.floodwatch.model.Shelter;
import tools.jackson.databind.json.JsonMapper;

@org.springframework.stereotype.Repository
public class ShelterRepository extends JsonFileRepository<Shelter> {
    public ShelterRepository(JsonStorage storage, JsonMapper json) {
        super(storage, json, "shelters.json", Shelter.class, Shelter::id);
    }
}
