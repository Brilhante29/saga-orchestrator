package com.portfolio.saga.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Saga {
    private final String id;
    private SagaStatus status;
    private final List<SagaStep<String, String>> steps;
    private final SagaLog sagaLog;
    private final String input;

    public Saga(List<SagaStep<String, String>> steps, SagaLog sagaLog, String input) {
        this.id = UUID.randomUUID().toString();
        this.status = SagaStatus.PENDING;
        this.steps = new ArrayList<>(steps);
        this.sagaLog = sagaLog;
        this.input = input;
    }

    public String getId() { return id; }
    public SagaStatus getStatus() { return status; }
    public void setStatus(SagaStatus status) { this.status = status; }
    public List<SagaStep<String, String>> getSteps() { return Collections.unmodifiableList(steps); }
    public SagaLog getSagaLog() { return sagaLog; }
    public String getInput() { return input; }
}
