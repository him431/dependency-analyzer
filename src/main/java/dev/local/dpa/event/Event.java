package dev.local.dpa.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Observed.class, name = "dependency_observed"),
        @JsonSubTypes.Type(value = Removed.class,  name = "dependency_removed"),
        @JsonSubTypes.Type(value = Meta.class,     name = "service_metadata"),
        @JsonSubTypes.Type(value = Heartbeat.class, name = "heartbeat")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class Event {
    private final String eventId;
    private final Instant timestamp;
}
