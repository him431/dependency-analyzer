package dev.local.dpa.ingest;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.local.dpa.config.AppProps;
import dev.local.dpa.persist.Snapshot;
import dev.local.dpa.persist.Wal;
import dev.local.dpa.queue.EventQueue;
import dev.local.dpa.tools.DataGen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class Coordinator {

    private static final Logger log = LoggerFactory.getLogger(Coordinator.class);

    private final AppProps props;
    private final EventQueue queue;
    private final Dedup dedup;
    private final Wal wal;
    private final Snapshot snapshot;
    private final EventApplier applier;
    private final Metrics metrics;
    private final ObjectMapper json;
    private final DataGen gen;

    private final AtomicBoolean producersUp = new AtomicBoolean(true);
    private final AtomicBoolean consumersUp = new AtomicBoolean(true);

    private ExecutorService producerPool;
    private ExecutorService consumerPool;
    private JsonlSource source;

    public Coordinator(AppProps props, EventQueue queue, Dedup dedup, Wal wal, Snapshot snapshot,
                       EventApplier applier, Metrics metrics, ObjectMapper json, DataGen gen) {
        this.props = props; this.queue = queue; this.dedup = dedup; this.wal = wal;
        this.snapshot = snapshot; this.applier = applier; this.metrics = metrics;
        this.json = json; this.gen = gen;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() throws IOException {
        boolean loaded = snapshot.loadIfPresent(applier);
        wal.disable();
        try {
            wal.replay(e -> { try { applier.apply(e); } catch (Exception ex) {} });
        } finally { wal.enable(); }

        consumerPool = Executors.newFixedThreadPool(props.getConsumers(), named("consumer"));
        for (int i = 0; i < props.getConsumers(); i++) {
            consumerPool.submit(new Consumer(queue, dedup, wal, applier, metrics, consumersUp));
        }

        Path src = Paths.get(props.getSourceFile());
        if (loaded) {
            log.info("warm boot from snapshot, file producers idle");
        } else {
            if (!Files.exists(src) && props.isAutoSeed()) {
                log.info("seeding {} events", props.getGenerator().getEvents());
                gen.toFile(props.getGenerator().getServices(),
                        props.getGenerator().getEvents(),
                        props.getGenerator().getSeed(), src);
            }
            if (Files.exists(src)) {
                source = new JsonlSource(src, json);
                producerPool = Executors.newFixedThreadPool(props.getProducers(), named("producer"));
                for (int i = 0; i < props.getProducers(); i++) {
                    producerPool.submit(new Producer("p" + i, queue, source, metrics, producersUp));
                }
            }
        }
        log.info("up: {}p/{}c, snapshot={}", props.getProducers(), props.getConsumers(), loaded);
    }

    @PreDestroy
    public void stop() throws Exception {
        producersUp.set(false);
        if (producerPool != null) { producerPool.shutdown(); producerPool.awaitTermination(10, TimeUnit.SECONDS); }
        if (source != null) source.close();
        long deadline = System.currentTimeMillis() + 15_000;
        while (queue.size() > 0 && System.currentTimeMillis() < deadline) Thread.sleep(50);
        consumersUp.set(false);
        if (consumerPool != null) { consumerPool.shutdown(); consumerPool.awaitTermination(15, TimeUnit.SECONDS); }
        try { snapshot.take(); } catch (Exception ignored) {}
        wal.close();
    }

    private static ThreadFactory named(String prefix) {
        AtomicInteger n = new AtomicInteger();
        return r -> new Thread(r, prefix + "-" + n.incrementAndGet());
    }
}
