package com.portfolio.saga.steps;

import com.portfolio.saga.domain.SagaStep;

public class ReserveInventoryStep implements SagaStep<String, String> {
    private final boolean shouldFail;

    public ReserveInventoryStep(boolean shouldFail) {
        this.shouldFail = shouldFail;
    }

    @Override
    public String getName() { return "reserve-inventory"; }

    @Override
    public String execute(String input) {
        if (shouldFail) {
            throw new RuntimeException("reserve-inventory: failed for order " + input);
        }
        return "reserve-inventory: reserved for order " + input;
    }

    @Override
    public void compensate(String input) {
    }
}
