package dev.local.dpa.ingest;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Component
public class Dedup {

    private final Set<String> seen = Collections.synchronizedSet(new HashSet<String>());

    public boolean firstSight(String eventId) { return seen.add(eventId); }
}
