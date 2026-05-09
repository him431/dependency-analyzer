package dev.local.dpa.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;

@Getter
@EqualsAndHashCode(callSuper = true)
public class Removed extends Event {
    private final String source;
    private final String target;

    @JsonCreator
    public Removed(@JsonProperty("event_id")  String eventId,
                   @JsonProperty("timestamp") Instant timestamp,
                   @JsonProperty("source")    String source,
                   @JsonProperty("target")    String target) {
        super(eventId, timestamp);
        this.source = source;
        this.target = target;
    }
}
