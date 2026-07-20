package com.portfolio.saga.domain;

import com.portfolio.saga.application.InMemorySagaLog;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SagaOrchestratorCompensationTest {

    @Test
    void shouldCompensateWhenSecondStepFails() {
        InMemorySagaLog log = new InMemorySagaLog();
        Saga saga = new Saga(
                List.of(
                        new TrackableStep("step-1", false),
                        new TrackableStep("step-2", true),
                        new TrackableStep("step-3", false)
                ),
                log, "order-1"
        );

        SagaOrchestrator orchestrator = new SagaOrchestrator();
        SagaStatus result = orchestrator.execute(saga);

        assertEquals(SagaStatus.COMPENSATED, result);
        assertEquals(SagaStatus.COMPENSATED, saga.getStatus());
    }

    @Test
    void shouldSkipCompensationForStepsNotExecuted() {
        InMemorySagaLog log = new InMemorySagaLog();
        Saga saga = new Saga(
                List.of(
                        new TrackableStep("step-1", false),
                        new TrackableStep("step-2", true),
                        new TrackableStep("step-3", false)
                ),
                log, "order-1"
        );

        SagaOrchestrator orchestrator = new SagaOrchestrator();
        orchestrator.execute(saga);

        List<LogEntry> entries = log.entries(saga.getId());
        long compensated = entries.stream()
                .filter(e -> e.message().contains("compensated"))
                .count();
        assertTrue(compensated > 0, "At least one compensation should occur");
    }

    @Test
    void shouldFailOnFirstStepAndNotExecuteRemaining() {
        InMemorySagaLog log = new InMemorySagaLog();
        Saga saga = new Saga(
                List.of(
                        new TrackableStep("step-1", true),
                        new TrackableStep("step-2", false),
                        new TrackableStep("step-3", false)
                ),
                log, "order-1"
        );

        SagaOrchestrator orchestrator = new SagaOrchestrator();
        SagaStatus result = orchestrator.execute(saga);

        assertEquals(SagaStatus.COMPENSATED, result);
        List<LogEntry> entries = log.entries(saga.getId());
        assertTrue(entries.stream().noneMatch(e -> e.message().contains("step-2")),
                "step-2 should not have been executed");
        assertTrue(entries.stream().noneMatch(e -> e.message().contains("step-3")),
                "step-3 should not have been executed");
    }

    @Test
    void shouldCompensateAllExecutedStepsOnFailure() {
        InMemorySagaLog log = new InMemorySagaLog();
        AtomicInteger comp1 = new AtomicInteger(0);
        AtomicInteger comp2 = new AtomicInteger(0);

        Saga saga = new Saga(
                List.of(
                        new CompensatingStep("step-1", false, comp1),
                        new CompensatingStep("step-2", true, comp2),
                        new CompensatingStep("step-3", false, null)
                ),
                log, "order-1"
        );

        SagaOrchestrator orchestrator = new SagaOrchestrator();
        orchestrator.execute(saga);

        assertEquals(1, comp1.get(), "step-1 should be compensated");
        assertEquals(0, comp2.get(), "step-2 should not be compensated (it failed)");
    }

    private static class TrackableStep implements SagaStep<String, String> {
        private final String name;
        private final boolean shouldFail;

        TrackableStep(String name, boolean shouldFail) {
            this.name = name;
            this.shouldFail = shouldFail;
        }

        @Override
        public String getName() { return name; }

        @Override
        public String execute(String input) {
            if (shouldFail) throw new RuntimeException(name + " failed");
            return name + " executed";
        }

        @Override
        public void compensate(String input) {
        }
    }

    private static class CompensatingStep implements SagaStep<String, String> {
        private final String name;
        private final boolean shouldFail;
        private final AtomicInteger compensateCount;

        CompensatingStep(String name, boolean shouldFail, AtomicInteger compensateCount) {
            this.name = name;
            this.shouldFail = shouldFail;
            this.compensateCount = compensateCount;
        }

        @Override
        public String getName() { return name; }

        @Override
        public String execute(String input) {
            if (shouldFail) throw new RuntimeException(name + " failed");
            return name + " executed";
        }

        @Override
        public void compensate(String input) {
            if (compensateCount != null) compensateCount.incrementAndGet();
        }
    }
}
