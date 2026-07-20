package com.portfolio.saga.domain;

public enum SagaStatus {
    PENDING,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED
}
