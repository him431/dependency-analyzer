package dev.local.dpa.algo;

import dev.local.dpa.graph.GraphFixtures;
import dev.local.dpa.graph.GraphStore;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AlgoTest {

    @Test
    void reach_y_shape() {
        GraphStore g = GraphFixtures.yShape();
        Reach r = new Reach(g);
        assertThat(r.downstream("A").keySet()).containsExactlyInAnyOrder("B", "C", "D");
        assertThat(r.upstream("D").keySet()).containsExactlyInAnyOrder("A", "B");
    }

    @Test
    void shortest_path_picks_low_latency() {
        // can reuse y-shape: A->B (10), B->C (20), B->D (30). path A->C should be 30.
        GraphStore g = GraphFixtures.yShape();
        Optional<ShortestPath.Result> r = new ShortestPath(g).find("A", "C");
        assertThat(r).isPresent();
        assertThat(r.get().getPath()).containsExactly("A", "B", "C");
    }

    @Test
    void cycles_finds_sccs_and_self_loop() {
        GraphStore g = GraphFixtures.withCycles();
        Cycles.Result r = new Cycles(g).find();
        assertThat(r.getStronglyConnectedComponents()).hasSize(2);
        assertThat(r.getSelfLoops()).contains("Z");
    }

    @Test
    void cycles_empty_on_acyclic() {
        Cycles.Result r = new Cycles(GraphFixtures.yShape()).find();
        assertThat(r.getStronglyConnectedComponents()).isEmpty();
        assertThat(r.getSelfLoops()).isEmpty();
    }
}
