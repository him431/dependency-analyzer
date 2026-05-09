package dev.local.dpa.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;

@Getter
@EqualsAndHashCode(callSuper = true)
public class Heartbeat extends Event {
    private final String service;

    @JsonCreator
    public Heartbeat(@JsonProperty("event_id")  String eventId,
                     @JsonProperty("timestamp") Instant timestamp,
                     @JsonProperty("service")   String service) {
        super(eventId, timestamp);
        this.service = service;
    }
}
