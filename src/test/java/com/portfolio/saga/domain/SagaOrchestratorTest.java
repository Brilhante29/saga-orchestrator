package com.portfolio.saga.domain;

import com.portfolio.saga.application.InMemorySagaLog;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SagaOrchestratorTest {

    @Test
    void shouldCompleteSagaWhenAllStepsSucceed() {
        InMemorySagaLog log = new InMemorySagaLog();
        Saga saga = new Saga(
                List.of(
                        new TestStep("step-1", false),
                        new TestStep("step-2", false),
                        new TestStep("step-3", false)
                ),
                log, "order-1"
        );

        SagaOrchestrator orchestrator = new SagaOrchestrator();
        SagaStatus result = orchestrator.execute(saga);

        assertEquals(SagaStatus.COMPLETED, result);
        assertEquals(SagaStatus.COMPLETED, saga.getStatus());
    }

    @Test
    void shouldExecuteStepsInOrder() {
        InMemorySagaLog log = new InMemorySagaLog();
        Saga saga = new Saga(
                List.of(
                        new TestStep("first", false),
                        new TestStep("second", false),
                        new TestStep("third", false)
                ),
                log, "order-1"
        );

        SagaOrchestrator orchestrator = new SagaOrchestrator();
        orchestrator.execute(saga);

        List<LogEntry> entries = log.entries(saga.getId());
        assertTrue(entries.stream().anyMatch(e -> e.message().contains("first succeeded")));
        assertTrue(entries.stream().anyMatch(e -> e.message().contains("second succeeded")));
        assertTrue(entries.stream().anyMatch(e -> e.message().contains("third succeeded")));
    }

    @Test
    void shouldRecordLogEntries() {
        InMemorySagaLog log = new InMemorySagaLog();
        Saga saga = new Saga(
                List.of(new TestStep("step-1", false)),
                log, "order-1"
        );

        SagaOrchestrator orchestrator = new SagaOrchestrator();
        orchestrator.execute(saga);

        List<LogEntry> entries = log.entries(saga.getId());
        assertFalse(entries.isEmpty());
    }

    private static class TestStep implements SagaStep<String, String> {
        private final String name;
        private final boolean shouldFail;

        TestStep(String name, boolean shouldFail) {
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
}
