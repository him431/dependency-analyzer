package dev.local.dpa.graph;

import dev.local.dpa.event.Status;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Getter
@RequiredArgsConstructor
public final class Sample {
    private final Instant ts;
    private final long latencyMs;
    private final Status status;
}
