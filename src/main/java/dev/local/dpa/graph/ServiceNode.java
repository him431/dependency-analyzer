package dev.local.dpa.graph;

import lombok.Getter;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Getter
public final class ServiceNode {
    private final String id;
    private volatile Map<String, String> attributes = Collections.emptyMap();
    private volatile Instant attributesUpdatedAt = Instant.EPOCH;
    private volatile Instant lastHeartbeat = Instant.EPOCH;

    public ServiceNode(String id) { this.id = id; }

    public synchronized void updateAttributes(Map<String, String> attrs, Instant ts) {
        if (ts.isBefore(attributesUpdatedAt)) return;
        Map<String, String> copy = attrs == null ? Collections.<String,String>emptyMap() : new HashMap<>(attrs);
        this.attributes = Collections.unmodifiableMap(copy);
        this.attributesUpdatedAt = ts;
    }

    public synchronized void heartbeat(Instant ts) {
        if (ts.isAfter(lastHeartbeat)) lastHeartbeat = ts;
    }
}
