package dev.local.dpa.queue;

import dev.local.dpa.event.Event;
import java.util.concurrent.TimeUnit;

public interface EventQueue {
    void put(Event e) throws InterruptedException;
    Event poll(long timeout, TimeUnit unit) throws InterruptedException;
    int size();
    int capacity();
}
