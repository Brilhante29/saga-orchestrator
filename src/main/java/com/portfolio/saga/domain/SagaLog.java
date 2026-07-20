package com.portfolio.saga.domain;

import java.util.List;

public interface SagaLog {
    void log(String sagaId, SagaStatus from, SagaStatus to, String message);
    List<LogEntry> entries(String sagaId);
    List<LogEntry> allEntries();
    void clear();
}
