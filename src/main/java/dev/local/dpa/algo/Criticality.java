package dev.local.dpa.algo;

import dev.local.dpa.graph.GraphStore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// score(v) = upstream_count(v) * downstream_count(v)
// = number of (s,t) pairs that route through v and would lose connectivity if v vanished.
// see REPORT for why this over exact betweenness.
@Component
public class Criticality {

    private final GraphStore graph;
    private final Reach reach;

    public Criticality(GraphStore graph, Reach reach) {
        this.graph = graph;
        this.reach = reach;
    }

    public List<Score> topK(int k) {
        if (k <= 0) return java.util.Collections.emptyList();
        List<Score> all = new ArrayList<>();
        for (String s : graph.services()) {
            long up = reach.countUpstream(s);
            long down = reach.countDownstream(s);
            if (up == 0 && down == 0) continue;
            all.add(new Score(s, up * down));
        }
        all.sort(Comparator.comparingLong(Score::getScore).reversed());
        return all.subList(0, Math.min(k, all.size()));
    }

    @Getter @RequiredArgsConstructor
    public static class Score {
        private final String service;
        private final long score;
    }
}
