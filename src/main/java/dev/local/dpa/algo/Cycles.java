package dev.local.dpa.algo;

import dev.local.dpa.graph.GraphStore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

// iterative Tarjan SCC -- avoids stack overflow on deep chains
@Component
public class Cycles {

    private final GraphStore graph;

    public Cycles(GraphStore graph) { this.graph = graph; }

    public Result find() {
        Map<String, Integer> idx = new HashMap<>();
        Map<String, Integer> low = new HashMap<>();
        Set<String> onStack = new HashSet<>();
        Deque<String> sccStack = new ArrayDeque<>();
        List<Set<String>> sccs = new ArrayList<>();
        List<String> selfLoops = new ArrayList<>();
        int[] counter = {0};

        for (String s : graph.services()) {
            if (graph.outgoing(s).containsKey(s)) selfLoops.add(s);
        }
        for (String s : graph.services()) {
            if (!idx.containsKey(s)) sc(s, idx, low, onStack, sccStack, sccs, counter);
        }
        return new Result(sccs, selfLoops);
    }

    private void sc(String start, Map<String, Integer> idx, Map<String, Integer> low,
                    Set<String> onStack, Deque<String> sccStack, List<Set<String>> sccs, int[] counter) {
        Deque<Frame> work = new ArrayDeque<>();
        push(start, work, idx, low, onStack, sccStack, counter);

        while (!work.isEmpty()) {
            Frame f = work.peek();
            if (f.it.hasNext()) {
                String w = f.it.next();
                if (!idx.containsKey(w)) push(w, work, idx, low, onStack, sccStack, counter);
                else if (onStack.contains(w)) low.put(f.node, Math.min(low.get(f.node), idx.get(w)));
            } else {
                work.pop();
                if (low.get(f.node).equals(idx.get(f.node))) {
                    Set<String> scc = new HashSet<>();
                    String w;
                    do {
                        w = sccStack.pop();
                        onStack.remove(w);
                        scc.add(w);
                    } while (!w.equals(f.node));
                    if (scc.size() > 1) sccs.add(scc);
                }
                if (!work.isEmpty()) {
                    Frame parent = work.peek();
                    low.put(parent.node, Math.min(low.get(parent.node), low.get(f.node)));
                }
            }
        }
    }

    private void push(String node, Deque<Frame> work, Map<String, Integer> idx, Map<String, Integer> low,
                      Set<String> onStack, Deque<String> sccStack, int[] counter) {
        idx.put(node, counter[0]);
        low.put(node, counter[0]);
        counter[0]++;
        sccStack.push(node);
        onStack.add(node);
        work.push(new Frame(node, graph.outgoing(node).keySet().iterator()));
    }

    private static final class Frame {
        final String node; final Iterator<String> it;
        Frame(String n, Iterator<String> it) { this.node = n; this.it = it; }
    }

    @Getter @RequiredArgsConstructor
    public static class Result {
        private final List<Set<String>> stronglyConnectedComponents;
        private final List<String> selfLoops;
    }
}
