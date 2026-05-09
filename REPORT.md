# Report

## Architecture

```
producers -> bounded queue -> consumers -> dedup -> WAL -> apply -> GraphStore
                                                                       |
                                                                  query API
                                                                       |
                                                              periodic snapshot
                                                              (atomic rename, truncates WAL)
```

Producers read events from a JSONL file and put them on the queue. The queue
is bounded so producers block when consumers fall behind. Consumers dedup by
`event_id`, append to the WAL, then apply to the graph.

The graph is two `ConcurrentHashMap`s: forward (`source -> {target -> Edge}`)
and reverse (`target -> {sources}`). Reverse map costs ~2x edge memory but
makes `dependents()` O(V+E) instead of O(V*E). Per-edge mutations happen
inside `forward.compute()` so the fwd+rev pair updates atomically.

Persistence: every 30s the graph dumps to JSON via tmp-file + atomic rename,
then the WAL is truncated. On boot we load the snapshot, then replay any
WAL entries on top.

## Queue

`ArrayBlockingQueue` behind an `EventQueue` interface. The spec forbids
brokers, not in-process primitives. Bounded -> producers block on `put()`
when full. Block, never drop.

## Out-of-order and duplicates

Each edge tracks `lastEventTs`. An older `Observed` records its sample but
doesn't change edge state. A `Removed` writes a tombstone with its ts; a
later older `Observed` is ignored. Tombstones expire after a TTL.

Duplicates drop in the consumer's bounded LRU dedup cache before the WAL
or graph see them.

## Criticality

`score(v) = upstream_count(v) * downstream_count(v)` -- the bipartite cut
v participates in: how many (s,t) pairs would lose connectivity if v
vanished. Defensible, simple, no sampling. O(V * (V+E)).

Brandes' exact betweenness is O(V*E) per source, too slow for a query API.
A sampled approximation is the next step if this metric isn't
discriminative enough.

## Algorithms

- Reach / dependents: BFS on forward / reverse adjacency.
- Shortest path: Dijkstra weighted by per-edge rolling-mean latency.
- Cycles: iterative Tarjan SCC. Reports SCCs of size >= 2 + self-loops.
  Iterative to avoid StackOverflow on deep chains. Picked Tarjan over
  Johnson's (exponential on hub-shaped graphs).
- Health: aggregate per-edge samples in window over incident edges,
  compute error rate + p95.

## Trade-offs

- Snapshot is JSON, not packed binary. Trivial swap if throughput needs.
- Dedup is in-memory only. A multi-replica deploy would need a shared store.
- Criticality is `up * down`, not betweenness. Documented above.

## Bugs found during testing

- Forward/reverse drift under concurrent observe+remove. Two non-atomic
  steps; remove could see only one side updated. Fixed by wrapping both
  in `forward.compute()` per (src, tgt).
- Dedup was a plain `HashSet`, unbounded. Switched to bounded LRU.
- Scheduled snapshotter fired immediately on startup and overwrote the
  on-disk snapshot before recovery loaded it. Added an `armed` flag set
  by the coordinator after recovery.

## What's next

- Shard by service id with consistent hashing for 100x event rate.
- Anomaly detection via per-edge EWMA on latency / error rate.
