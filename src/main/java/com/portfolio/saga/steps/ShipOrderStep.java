package com.portfolio.saga.steps;

import com.portfolio.saga.domain.SagaStep;

public class ShipOrderStep implements SagaStep<String, String> {
    private final boolean shouldFail;

    public ShipOrderStep(boolean shouldFail) {
        this.shouldFail = shouldFail;
    }

    @Override
    public String getName() { return "ship-order"; }

    @Override
    public String execute(String input) {
        if (shouldFail) {
            throw new RuntimeException("ship-order: failed for order " + input);
        }
        return "ship-order: shipped for order " + input;
    }

    @Override
    public void compensate(String input) {
    }
}
