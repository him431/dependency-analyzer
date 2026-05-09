package dev.local.dpa.api;

import dev.local.dpa.algo.Criticality;
import dev.local.dpa.algo.Cycles;
import dev.local.dpa.algo.Health;
import dev.local.dpa.algo.Reach;
import dev.local.dpa.algo.ShortestPath;
import dev.local.dpa.graph.GraphStore;
import dev.local.dpa.ingest.Metrics;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/v1")
public class QueryController {

    private final GraphStore graph;
    private final Reach reach;
    private final ShortestPath shortest;
    private final Cycles cycles;
    private final Criticality criticality;
    private final Health health;
    private final Metrics metrics;

    public QueryController(GraphStore graph, Reach reach, ShortestPath shortest,
                           Cycles cycles, Criticality criticality, Health health, Metrics metrics) {
        this.graph = graph; this.reach = reach; this.shortest = shortest;
        this.cycles = cycles; this.criticality = criticality; this.health = health; this.metrics = metrics;
    }

    @GetMapping("/services/{id}/reachable")
    public ReachResp reachable(@PathVariable String id) {
        require(id);
        long t = System.nanoTime();
        Map<String, List<String>> p = reach.downstream(id);
        time(t);
        return new ReachResp(id, p.size(), p);
    }

    @GetMapping("/services/{id}/dependents")
    public ReachResp dependents(@PathVariable String id) {
        require(id);
        long t = System.nanoTime();
        Map<String, List<String>> p = reach.upstream(id);
        time(t);
        return new ReachResp(id, p.size(), p);
    }

    @GetMapping("/path")
    public PathResp path(@RequestParam String from, @RequestParam String to) {
        require(from); require(to);
        long t = System.nanoTime();
        Optional<ShortestPath.Result> r = shortest.find(from, to);
        time(t);
        return r.map(x -> new PathResp(from, to, true, x.getPath(), x.getTotalLatencyMs()))
                .orElseGet(() -> new PathResp(from, to, false, Collections.<String>emptyList(), 0.0));
    }

    @GetMapping("/cycles")
    public CycleResp cyclesEndpoint() {
        long t = System.nanoTime();
        Cycles.Result r = cycles.find();
        time(t);
        return new CycleResp(r.getStronglyConnectedComponents(), r.getSelfLoops());
    }

    @GetMapping("/critical")
    public CritResp critical(@RequestParam(defaultValue = "10") int k) {
        if (k <= 0 || k > 1000) throw new IllegalArgumentException("k must be in (0, 1000]");
        long t = System.nanoTime();
        List<Criticality.Score> top = criticality.topK(k);
        time(t);
        return new CritResp(k, "upstream_count * downstream_count", top);
    }

    @GetMapping("/services/{id}/health")
    public HealthResp healthEndpoint(@PathVariable String id,
                                     @RequestParam(defaultValue = "300") long windowSeconds) {
        require(id);
        if (windowSeconds <= 0) throw new IllegalArgumentException("windowSeconds must be > 0");
        long t = System.nanoTime();
        Health.Result r = health.of(id, Duration.ofSeconds(windowSeconds));
        time(t);
        return new HealthResp(r.getService(), r.getWindowSeconds(),
                r.getErrorRate(), r.getP95LatencyMs(), r.getSampleCount());
    }

    private void require(String id) {
        if (!graph.hasService(id)) throw new UnknownService(id);
    }

    private void time(long start) {
        metrics.getQueryLatency().record(Duration.ofNanos(System.nanoTime() - start));
    }

    @Getter @AllArgsConstructor public static class ReachResp {
        private String service; private int count; private Map<String, List<String>> paths;
    }
    @Getter @AllArgsConstructor public static class PathResp {
        private String from; private String to; private boolean found;
        private List<String> path; private double totalLatencyMs;
    }
    @Getter @AllArgsConstructor public static class CycleResp {
        private List<Set<String>> stronglyConnectedComponents; private List<String> selfLoops;
    }
    @Getter @AllArgsConstructor public static class CritResp {
        private int k; private String metric; private List<Criticality.Score> services;
    }
    @Getter @AllArgsConstructor public static class HealthResp {
        private String service; private long windowSeconds;
        private double errorRate; private double p95LatencyMs; private int sampleCount;
    }
}
