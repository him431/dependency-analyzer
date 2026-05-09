package dev.local.dpa.ingest;

import dev.local.dpa.event.Event;
import dev.local.dpa.queue.EventQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

public class Producer implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(Producer.class);

    private final String name;
    private final EventQueue queue;
    private final Iterator<Event> source;
    private final Metrics metrics;
    private final AtomicBoolean running;

    public Producer(String name, EventQueue queue, Iterator<Event> source, Metrics metrics, AtomicBoolean running) {
        this.name = name; this.queue = queue; this.source = source;
        this.metrics = metrics; this.running = running;
    }

    public void run() {
        try {
            while (running.get()) {
                Event e;
                synchronized (source) {
                    if (!source.hasNext()) break;
                    e = source.next();
                }
                if (e == null) continue;
                queue.put(e);
                metrics.getPublished().increment();
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            log.error("producer {} error", name, ex);
        }
    }
}
