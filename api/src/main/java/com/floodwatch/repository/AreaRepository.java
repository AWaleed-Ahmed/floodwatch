package com.floodwatch.repository;

import com.floodwatch.model.Area;
import tools.jackson.databind.json.JsonMapper;

@org.springframework.stereotype.Repository
public class AreaRepository extends JsonFileRepository<Area> {
    public AreaRepository(JsonStorage storage, JsonMapper json) {
        super(storage, json, "areas.json", Area.class, Area::id);
    }
}
