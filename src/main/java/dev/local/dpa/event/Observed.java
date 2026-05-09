package dev.local.dpa.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;

@Getter
@EqualsAndHashCode(callSuper = true)
public class Observed extends Event {
    private final String source;
    private final String target;
    private final long latencyMs;
    private final Status status;

    @JsonCreator
    public Observed(@JsonProperty("event_id")   String eventId,
                    @JsonProperty("timestamp")  Instant timestamp,
                    @JsonProperty("source")     String source,
                    @JsonProperty("target")     String target,
                    @JsonProperty("latency_ms") long latencyMs,
                    @JsonProperty("status")     Status status) {
        super(eventId, timestamp);
        this.source = source;
        this.target = target;
        this.latencyMs = latencyMs;
        this.status = status;
    }
}
