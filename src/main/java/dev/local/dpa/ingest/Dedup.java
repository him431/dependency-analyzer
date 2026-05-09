package dev.local.dpa.ingest;

import dev.local.dpa.config.AppProps;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component
public class Dedup {

    private final Set<String> seen;

    public Dedup(AppProps p) {
        final int cap = p.getDedupCapacity();
        Map<String, Boolean> lru = new LinkedHashMap<String, Boolean>(cap, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                return size() > cap;
            }
        };
        this.seen = Collections.synchronizedSet(Collections.newSetFromMap(lru));
    }

    public boolean firstSight(String eventId) { return seen.add(eventId); }
}
