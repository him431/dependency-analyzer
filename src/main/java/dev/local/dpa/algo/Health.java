package dev.local.dpa.algo;

import dev.local.dpa.graph.Edge;
import dev.local.dpa.graph.GraphStore;
import dev.local.dpa.graph.RollingStats;
import dev.local.dpa.graph.Sample;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class Health {

    private final GraphStore graph;

    public Health(GraphStore graph) { this.graph = graph; }

    public Result of(String service, Duration window) {
        if (!graph.hasService(service)) return null;
        Instant cutoff = Instant.now().minus(window);
        Set<Edge> edges = graph.incidentEdges(service);
        List<Sample> all = new ArrayList<>();
        for (Edge e : edges) all.addAll(e.getStats().samplesSince(cutoff));
        return new Result(service, window.getSeconds(),
                RollingStats.errorRate(all), RollingStats.p95(all), all.size());
    }

    @Getter @RequiredArgsConstructor
    public static class Result {
        private final String service;
        private final long windowSeconds;
        private final double errorRate;
        private final double p95LatencyMs;
        private final int sampleCount;
    }
}
