package com.portfolio.saga.domain;

import java.time.Instant;

public record LogEntry(String sagaId, SagaStatus from, SagaStatus to, String message, Instant timestamp) {
}
