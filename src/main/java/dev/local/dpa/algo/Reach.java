package dev.local.dpa.algo;

import dev.local.dpa.graph.GraphStore;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class Reach {

    private final GraphStore graph;

    public Reach(GraphStore graph) { this.graph = graph; }

    public Map<String, List<String>> downstream(String source) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        if (!graph.hasService(source)) return result;

        Map<String, String> parent = new HashMap<>();
        Deque<String> q = new ArrayDeque<>();
        q.add(source);
        parent.put(source, null);

        while (!q.isEmpty()) {
            String cur = q.removeFirst();
            for (String n : graph.outgoing(cur).keySet()) {
                if (parent.containsKey(n)) continue;
                parent.put(n, cur);
                q.addLast(n);
                List<String> p = new ArrayList<>();
                for (String c = n; c != null; c = parent.get(c)) p.add(c);
                Collections.reverse(p);
                result.put(n, p);
            }
        }
        return result;
    }
}
