package dev.local.dpa.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProps {
    private int queueCapacity = 8192;
    private int producers = 2;
    private int consumers = 4;
    private String sourceFile = "data/events.jsonl";
    private boolean autoSeed = true;
    private int dedupCapacity = 100_000;
    private int rollingBuffer = 64;
    private long tombstoneTtlSeconds = 3600;
    private String dataDir = "data";
    private long snapshotIntervalSeconds = 30;
    private Generator generator = new Generator();

    @Data public static class Generator {
        private int services = 3000;
        private int events = 30000;
        private long seed = 42;
    }
}
