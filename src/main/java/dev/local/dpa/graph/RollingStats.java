package dev.local.dpa.graph;

import dev.local.dpa.event.Status;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class RollingStats {
    private final Sample[] buf;
    private int writeIdx = 0;
    private int filled = 0;

    public RollingStats(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException();
        this.buf = new Sample[capacity];
    }

    public synchronized void add(Sample s) {
        buf[writeIdx] = s;
        writeIdx = (writeIdx + 1) % buf.length;
        if (filled < buf.length) filled++;
    }

    public synchronized double meanLatency() {
        if (filled == 0) return 0;
        long sum = 0;
        for (int i = 0; i < filled; i++) sum += buf[i].getLatencyMs();
        return (double) sum / filled;
    }

    public synchronized List<Sample> samplesSince(Instant cutoff) {
        if (filled == 0) return Collections.emptyList();
        List<Sample> out = new ArrayList<>(filled);
        for (int i = 0; i < filled; i++) {
            Sample s = buf[i];
            if (s != null && !s.getTs().isBefore(cutoff)) out.add(s);
        }
        return out;
    }

    public synchronized int size() { return filled; }

    public static double errorRate(List<Sample> samples) {
        if (samples.isEmpty()) return 0;
        int errs = 0;
        for (Sample s : samples) if (s.getStatus() != Status.OK) errs++;
        return (double) errs / samples.size();
    }

    public static double p95(List<Sample> samples) {
        if (samples.isEmpty()) return 0;
        long[] arr = new long[samples.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = samples.get(i).getLatencyMs();
        Arrays.sort(arr);
        int idx = (int) Math.ceil(arr.length * 0.95) - 1;
        if (idx < 0) idx = 0;
        if (idx >= arr.length) idx = arr.length - 1;
        return arr[idx];
    }
}
