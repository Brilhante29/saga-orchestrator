package com.portfolio.saga.domain;

import java.util.List;

public class SagaOrchestrator {

    public SagaStatus execute(Saga saga) {
        List<SagaStep<String, String>> steps = saga.getSteps();
        SagaLog log = saga.getSagaLog();
        String sagaId = saga.getId();
        String input = saga.getInput();

        saga.setStatus(SagaStatus.PENDING);
        log.log(sagaId, SagaStatus.PENDING, SagaStatus.PENDING, "Saga started for input: " + input);

        int executedCount = 0;
        for (SagaStep<String, String> step : steps) {
            try {
                step.execute(input);
                executedCount++;
                log.log(sagaId, saga.getStatus(), SagaStatus.PENDING, step.getName() + " succeeded");
            } catch (Exception e) {
                log.log(sagaId, SagaStatus.PENDING, SagaStatus.FAILED,
                        step.getName() + " failed: " + e.getMessage());
                saga.setStatus(SagaStatus.COMPENSATING);
                log.log(sagaId, SagaStatus.FAILED, SagaStatus.COMPENSATING,
                        "Starting compensation for " + sagaId);

                for (int i = executedCount - 1; i >= 0; i--) {
                    SagaStep<String, String> executedStep = steps.get(i);
                    try {
                        executedStep.compensate(input);
                        log.log(sagaId, SagaStatus.COMPENSATING, SagaStatus.COMPENSATING,
                                executedStep.getName() + " compensated");
                    } catch (Exception ce) {
                        log.log(sagaId, SagaStatus.COMPENSATING, SagaStatus.FAILED,
                                "Compensation failed for " + executedStep.getName() + ": " + ce.getMessage());
                    }
                }

                saga.setStatus(SagaStatus.COMPENSATED);
                log.log(sagaId, SagaStatus.COMPENSATING, SagaStatus.COMPENSATED, "Saga compensated");
                return SagaStatus.COMPENSATED;
            }
        }

        saga.setStatus(SagaStatus.COMPLETED);
        log.log(sagaId, SagaStatus.PENDING, SagaStatus.COMPLETED, "Saga completed");
        return SagaStatus.COMPLETED;
    }
}
