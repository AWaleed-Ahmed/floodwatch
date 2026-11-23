package com.floodwatch.integration;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/** Checks Open-Meteo responses before we use them, so bad data never reaches a chart. */
@Component
public class JsonValidator {

    /** Each field is a dot path, e.g. "hourly.time". */
    public void requireFields(JsonNode root, String... fields) {
        for (String field : fields) {
            JsonNode node = root;
            for (String part : field.split("\\.")) {
                node = node.path(part);
            }
            if (node.isMissingNode() || node.isNull()) {
                throw new ExternalDataException("Missing field in API response: " + field);
            }
        }
    }

    public void requireSameLength(JsonNode a, JsonNode b, String what) {
        if (a.size() != b.size()) {
            throw new ExternalDataException("Mismatched array lengths in " + what);
        }
    }

    /** A forecast whose first time stamp is more than 2 days away from today is treated as wrong. */
    public void requireNearNow(Instant firstTime) {
        Instant now = Instant.now();
        if (firstTime.isBefore(now.minus(2, ChronoUnit.DAYS)) || firstTime.isAfter(now.plus(2, ChronoUnit.DAYS))) {
            throw new ExternalDataException("Forecast time stamps are not current: " + firstTime);
        }
    }
}
