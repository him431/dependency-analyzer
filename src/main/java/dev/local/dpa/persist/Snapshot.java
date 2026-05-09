package dev.local.dpa.persist;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.local.dpa.config.AppProps;
import dev.local.dpa.event.Meta;
import dev.local.dpa.event.Observed;
import dev.local.dpa.event.Status;
import dev.local.dpa.graph.Edge;
import dev.local.dpa.graph.GraphStore;
import dev.local.dpa.graph.Sample;
import dev.local.dpa.graph.ServiceNode;
import dev.local.dpa.ingest.EventApplier;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class Snapshot {

    private static final Logger log = LoggerFactory.getLogger(Snapshot.class);

    private final AppProps props;
    private final ObjectMapper json;
    private final GraphStore graph;
    private final Wal wal;
    private volatile boolean armed = false;

    public Snapshot(AppProps props, ObjectMapper json, GraphStore graph, Wal wal) {
        this.props = props; this.json = json; this.graph = graph; this.wal = wal;
    }

    public void arm() { armed = true; }

    @Scheduled(fixedDelayString = "${app.snapshot-interval-seconds:30}000")
    public void scheduled() {
        if (!armed) return;
        try { take(); } catch (Exception e) { log.warn("snapshot failed: {}", e.toString()); }
    }

    public synchronized void take() throws IOException {
        Path dir = Paths.get(props.getDataDir());
        Files.createDirectories(dir);
        Path target = dir.resolve("snapshot.json");
        Path tmp = dir.resolve("snapshot.json.tmp");
        Dump d = capture();
        Files.write(tmp, json.writeValueAsBytes(d));
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        wal.truncate();
        log.info("snapshot: {} services / {} edges", d.services.size(), d.edges.size());
    }

    public synchronized boolean loadIfPresent(EventApplier applier) {
        Path target = Paths.get(props.getDataDir(), "snapshot.json");
        if (!Files.exists(target)) return false;
        try {
            Dump d = json.readValue(Files.readAllBytes(target), Dump.class);
            wal.disable();
            try {
                for (S s : d.services) {
                    if (s.attributes != null && !s.attributes.isEmpty()) {
                        applier.apply(new Meta("snap-" + s.id, s.attributesUpdatedAt, s.id, s.attributes));
                    }
                }
                for (E e : d.edges) {
                    for (Smp s : e.samples) {
                        applier.apply(new Observed("snap-" + e.source + ">" + e.target + "@" + s.ts.toEpochMilli(),
                                s.ts, e.source, e.target, s.latencyMs, s.status));
                    }
                }
            } finally { wal.enable(); }
            log.info("loaded snapshot: {} services / {} edges", d.services.size(), d.edges.size());
            return true;
        } catch (IOException ioe) { throw new UncheckedIOException(ioe); }
    }

    private Dump capture() {
        Dump d = new Dump();
        for (String svc : graph.services()) {
            ServiceNode n = graph.node(svc);
            d.services.add(new S(n.getId(), n.getAttributes(), n.getAttributesUpdatedAt()));
            for (Edge e : graph.outgoing(svc).values()) {
                E ed = new E();
                ed.source = e.getSource();
                ed.target = e.getTarget();
                for (Sample s : e.getStats().samplesSince(Instant.EPOCH)) {
                    ed.samples.add(new Smp(s.getTs(), s.getLatencyMs(), s.getStatus()));
                }
                d.edges.add(ed);
            }
        }
        return d;
    }

    @Data @NoArgsConstructor public static class Dump {
        public List<S> services = new ArrayList<>();
        public List<E> edges = new ArrayList<>();
    }
    @Data @NoArgsConstructor @AllArgsConstructor public static class S {
        public String id; public Map<String,String> attributes = new HashMap<>(); public Instant attributesUpdatedAt;
    }
    @Data @NoArgsConstructor public static class E {
        public String source; public String target; public List<Smp> samples = new ArrayList<>();
    }
    @Data @NoArgsConstructor @AllArgsConstructor public static class Smp {
        public Instant ts; public long latencyMs; public Status status;
    }
}
