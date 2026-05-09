package dev.local.dpa.ingest;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.local.dpa.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class JsonlSource implements Iterator<Event>, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(JsonlSource.class);

    private final BufferedReader reader;
    private final ObjectMapper json;
    private Event next;
    private boolean closed = false;

    public JsonlSource(Path file, ObjectMapper json) throws IOException {
        this.reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
        this.json = json;
        advance();
    }

    private void advance() {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;
                try { next = json.readValue(line, Event.class); return; }
                catch (IOException ioe) { log.warn("bad line: {}", ioe.toString()); }
            }
            next = null;
            close();
        } catch (IOException ioe) { throw new UncheckedIOException(ioe); }
    }

    public boolean hasNext() { return next != null; }

    public Event next() {
        if (next == null) throw new NoSuchElementException();
        Event e = next; advance(); return e;
    }

    public void close() {
        if (closed) return; closed = true;
        try { reader.close(); } catch (IOException ignored) {}
    }
}
