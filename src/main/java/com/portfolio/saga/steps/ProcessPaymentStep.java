package com.portfolio.saga.steps;

import com.portfolio.saga.domain.SagaStep;

public class ProcessPaymentStep implements SagaStep<String, String> {
    private final boolean shouldFail;

    public ProcessPaymentStep(boolean shouldFail) {
        this.shouldFail = shouldFail;
    }

    @Override
    public String getName() { return "process-payment"; }

    @Override
    public String execute(String input) {
        if (shouldFail) {
            throw new RuntimeException("process-payment: failed for order " + input);
        }
        return "process-payment: processed for order " + input;
    }

    @Override
    public void compensate(String input) {
    }
}
