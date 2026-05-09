package dev.local.dpa.queue;

import dev.local.dpa.config.AppProps;
import dev.local.dpa.event.Event;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@Component
public class BlockingEventQueue implements EventQueue {

    private final BlockingQueue<Event> q;
    private final int cap;

    public BlockingEventQueue(AppProps p) {
        this.cap = p.getQueueCapacity();
        this.q = new ArrayBlockingQueue<>(cap);
    }

    public void put(Event e) throws InterruptedException { q.put(e); }
    public Event poll(long t, TimeUnit u) throws InterruptedException { return q.poll(t, u); }
    public int size() { return q.size(); }
    public int capacity() { return cap; }
}
