package dev.local.dpa.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.local.dpa.event.Event;
import dev.local.dpa.event.Heartbeat;
import dev.local.dpa.event.Meta;
import dev.local.dpa.event.Observed;
import dev.local.dpa.event.Removed;
import dev.local.dpa.event.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

@Component
public class DataGen {

    private static final Logger log = LoggerFactory.getLogger(DataGen.class);

    private final ObjectMapper json;

    public DataGen(ObjectMapper json) { this.json = json; }

    public void toFile(int services, int events, long seed, Path out) {
        try {
            Files.createDirectories(out.getParent());
            try (BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
                for (Event e : generate(services, events, seed)) {
                    w.write(json.writeValueAsString(e));
                    w.newLine();
                }
            }
            log.info("wrote {}", out.toAbsolutePath());
        } catch (IOException ioe) { throw new UncheckedIOException(ioe); }
    }

    public List<Event> generate(int serviceCount, int eventCount, long seed) {
        return generate(serviceCount, eventCount, seed, Instant.now().minusSeconds(3600));
    }

    public List<Event> generate(int serviceCount, int eventCount, long seed, Instant base) {
        if (serviceCount <= 0 || eventCount <= 0) throw new IllegalArgumentException();
        Random r = new Random(seed);

        List<String> svcs = makeServices(serviceCount, r);
        List<String> hubs = pickHubs(svcs, r);
        Set<String> flaky = pickFlaky(svcs, r);
        Map<String, List<String>> deps = wireDeps(svcs, hubs, r);
        injectCycles(deps, svcs, r);

        List<Event> out = new ArrayList<>(eventCount);
        String[] tiers = {"critical", "important", "best-effort"};
        String[] regions = {"us-east", "us-west", "eu-west", "ap-south"};

        for (String s : svcs) {
            Map<String, String> a = new HashMap<>();
            a.put("team", "team-" + (Math.abs(s.hashCode()) % 25));
            a.put("tier", tiers[r.nextInt(tiers.length)]);
            a.put("region", regions[r.nextInt(regions.length)]);
            out.add(new Meta(eid(r), base.plusSeconds(r.nextInt(60)), s, a));
        }

        Instant t = base.plusSeconds(120);
        int obs = (int)(eventCount * 0.85);
        for (int i = 0; i < obs; i++) {
            t = t.plusMillis(50 + r.nextInt(200));
            String src = svcs.get(r.nextInt(svcs.size()));
            List<String> tgts = deps.get(src);
            if (tgts == null || tgts.isEmpty()) continue;
            String tgt = tgts.get(r.nextInt(tgts.size()));
            Status st = pickStatus(r, flaky.contains(src) || flaky.contains(tgt));
            Instant ts = (r.nextInt(100) < 2) ? t.minusSeconds(1 + r.nextInt(30)) : t;
            out.add(new Observed(eid(r), ts, src, tgt, latency(r), st));
        }

        int hb = (int)(eventCount * 0.10);
        for (int i = 0; i < hb; i++) {
            t = t.plusMillis(100 + r.nextInt(500));
            out.add(new Heartbeat(eid(r), t, svcs.get(r.nextInt(svcs.size()))));
        }

        int rm = Math.max(5, (int)(eventCount * 0.005));
        for (int i = 0; i < rm; i++) {
            t = t.plusMillis(200 + r.nextInt(800));
            String src = svcs.get(r.nextInt(svcs.size()));
            List<String> tgts = deps.get(src);
            if (tgts == null || tgts.isEmpty()) continue;
            out.add(new Removed(eid(r), t, src, tgts.get(r.nextInt(tgts.size()))));
        }

        // ~1% deliberate dupes
        int dup = Math.max(1, out.size() / 100);
        List<Event> snap = new ArrayList<>(out);
        for (int i = 0; i < dup; i++) out.add(snap.get(r.nextInt(snap.size())));

        Collections.shuffle(out, r);
        log.info("generated {} events / {} services (seed={})", out.size(), svcs.size(), seed);
        return out;
    }

    private List<String> makeServices(int n, Random r) {
        Set<String> seed = new LinkedHashSet<>(Arrays.asList(
                "auth-service", "user-service", "session-service",
                "payments-service", "billing-service", "ledger-service",
                "checkout-api", "cart-service", "inventory-service",
                "search-service", "catalog-service",
                "notification-service", "email-service",
                "postgres-primary", "postgres-replica", "redis-cache",
                "feature-flags", "config-service"));
        String[] roles = {"api", "worker", "gateway", "indexer", "syncer", "reporter"};
        int i = 0;
        while (seed.size() < n) {
            seed.add("team" + r.nextInt(40) + "-" + roles[r.nextInt(roles.length)] + "-" + i++);
        }
        return new ArrayList<>(new TreeSet<>(seed)).subList(0, n);
    }

    private List<String> pickHubs(List<String> svcs, Random r) {
        List<String> h = new ArrayList<>();
        for (String s : Arrays.asList("auth-service", "postgres-primary", "redis-cache",
                                      "feature-flags", "user-service")) {
            if (svcs.contains(s)) h.add(s);
        }
        while (h.size() < 15 && h.size() < svcs.size()) {
            String pick = svcs.get(r.nextInt(svcs.size()));
            if (!h.contains(pick)) h.add(pick);
        }
        return h;
    }

    private Set<String> pickFlaky(List<String> svcs, Random r) {
        Set<String> out = new LinkedHashSet<>();
        int target = Math.min(svcs.size(), Math.max(3, svcs.size() / 100));
        int tries = 0;
        while (out.size() < target && tries++ < target * 10) {
            out.add(svcs.get(r.nextInt(svcs.size())));
        }
        return out;
    }

    private Map<String, List<String>> wireDeps(List<String> svcs, List<String> hubs, Random r) {
        Map<String, List<String>> deps = new HashMap<>();
        Set<String> hubSet = new HashSet<>(hubs);
        for (String s : svcs) {
            if (hubSet.contains(s)) { deps.put(s, new ArrayList<>()); continue; }
            int deg = Math.min(1 + r.nextInt(4), svcs.size() - 1);
            Set<String> tgts = new LinkedHashSet<>();
            if (!hubs.isEmpty()) {
                for (int i = 0; i < Math.max(1, deg / 2); i++) {
                    String h = hubs.get(r.nextInt(hubs.size()));
                    if (!h.equals(s)) tgts.add(h);
                }
            }
            int tries = 0;
            while (tgts.size() < deg && tries++ < deg * 20) {
                String t = svcs.get(r.nextInt(svcs.size()));
                if (!t.equals(s)) tgts.add(t);
            }
            deps.put(s, new ArrayList<>(tgts));
        }
        return deps;
    }

    private void injectCycles(Map<String, List<String>> deps, List<String> svcs, Random r) {
        List<String> pool = new ArrayList<>();
        for (String s : svcs) {
            List<String> outs = deps.get(s);
            if (outs != null && !outs.isEmpty()) pool.add(s);
        }
        if (pool.size() < 3) return;
        for (int sz : new int[]{3, 4, 5, 3}) {
            if (sz > pool.size()) continue;
            Set<String> picked = new LinkedHashSet<>();
            int tries = 0;
            while (picked.size() < sz && tries++ < 200) picked.add(pool.get(r.nextInt(pool.size())));
            List<String> ring = new ArrayList<>(picked);
            for (int i = 0; i < ring.size(); i++) {
                List<String> ts = deps.get(ring.get(i));
                String b = ring.get((i + 1) % ring.size());
                if (!ts.contains(b)) ts.add(b);
            }
        }
    }

    private long latency(Random r) {
        double v = Math.exp(3.4 + 0.9 * r.nextGaussian());
        return Math.min(8000, Math.max(1, Math.round(v)));
    }

    private Status pickStatus(Random r, boolean flaky) {
        int p = r.nextInt(1000);
        if (flaky) { if (p < 80) return Status.ERROR; if (p < 110) return Status.TIMEOUT; return Status.OK; }
        if (p < 12) return Status.ERROR;
        if (p < 18) return Status.TIMEOUT;
        return Status.OK;
    }

    private String eid(Random r) { return "e-" + Long.toHexString(r.nextLong()); }
}
