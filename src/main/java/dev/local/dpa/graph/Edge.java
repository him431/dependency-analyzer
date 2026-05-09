package dev.local.dpa.graph;

import lombok.Getter;

import java.time.Instant;

@Getter
public final class Edge {
    private final String source;
    private final String target;
    private final RollingStats stats;
    private volatile Instant lastEventTs;

    public Edge(String source, String target, int statsCapacity, Instant lastEventTs) {
        this.source = source;
        this.target = target;
        this.stats = new RollingStats(statsCapacity);
        this.lastEventTs = lastEventTs;
    }

    public void touch(Instant ts) {
        if (ts.isAfter(lastEventTs)) lastEventTs = ts;
    }
}
