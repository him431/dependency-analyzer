package dev.local.dpa.event;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Status {
    @JsonProperty("ok")      OK,
    @JsonProperty("error")   ERROR,
    @JsonProperty("timeout") TIMEOUT
}
