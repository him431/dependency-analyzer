package dev.local.dpa.api;

public class UnknownService extends RuntimeException {
    public UnknownService(String id) { super("unknown service: " + id); }
}
