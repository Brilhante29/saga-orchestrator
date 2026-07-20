package com.portfolio.saga.domain;

public interface SagaStep<T, R> {
    String getName();
    R execute(T input);
    void compensate(T input);
}
