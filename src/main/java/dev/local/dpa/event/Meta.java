package dev.local.dpa.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

@Getter
@EqualsAndHashCode(callSuper = true)
public class Meta extends Event {
    private final String service;
    private final Map<String, String> attributes;

    @JsonCreator
    public Meta(@JsonProperty("event_id")   String eventId,
                @JsonProperty("timestamp")  Instant timestamp,
                @JsonProperty("service")    String service,
                @JsonProperty("attributes") Map<String, String> attributes) {
        super(eventId, timestamp);
        this.service = service;
        this.attributes = attributes == null ? Collections.<String,String>emptyMap()
                : Collections.unmodifiableMap(attributes);
    }
}
