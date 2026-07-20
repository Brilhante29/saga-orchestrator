package com.portfolio.saga.application;

import com.portfolio.saga.domain.LogEntry;
import com.portfolio.saga.domain.SagaLog;
import com.portfolio.saga.domain.SagaStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class InMemorySagaLog implements SagaLog {
    private final ConcurrentLinkedQueue<LogEntry> entries = new ConcurrentLinkedQueue<>();

    @Override
    public void log(String sagaId, SagaStatus from, SagaStatus to, String message) {
        entries.add(new LogEntry(sagaId, from, to, message, Instant.now()));
    }

    @Override
    public List<LogEntry> entries(String sagaId) {
        return entries.stream()
                .filter(e -> e.sagaId().equals(sagaId))
                .collect(Collectors.toList());
    }

    @Override
    public List<LogEntry> allEntries() {
        return new ArrayList<>(entries);
    }

    @Override
    public void clear() {
        entries.clear();
    }
}
