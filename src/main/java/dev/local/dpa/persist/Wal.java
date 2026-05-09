package dev.local.dpa.persist;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.local.dpa.config.AppProps;
import dev.local.dpa.event.Event;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;

@Component
public class Wal {

    private final AppProps props;
    private final ObjectMapper json;
    private Path path;
    private BufferedWriter writer;
    private boolean disabled = false;

    public Wal(AppProps props, ObjectMapper json) { this.props = props; this.json = json; }

    @PostConstruct
    public void init() throws IOException {
        Path dir = Paths.get(props.getDataDir());
        Files.createDirectories(dir);
        this.path = dir.resolve("wal.log");
        this.writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public synchronized void append(Event e) {
        if (disabled) return;
        try {
            writer.write(json.writeValueAsString(e));
            writer.newLine();
            writer.flush();
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    public void replay(Consumer<Event> handler) throws IOException {
        if (!Files.exists(path)) return;
        try (BufferedReader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isEmpty()) continue;
                try { handler.accept(json.readValue(line, Event.class)); }
                catch (IOException ignored) { /* skip bad line */ }
            }
        }
    }

    public synchronized void truncate() throws IOException {
        if (writer != null) writer.close();
        Files.write(path, new byte[0]);
        this.writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public synchronized void disable() { disabled = true; }
    public synchronized void enable() { disabled = false; }

    public synchronized void close() throws IOException {
        if (writer != null) { writer.flush(); writer.close(); writer = null; }
    }
}
