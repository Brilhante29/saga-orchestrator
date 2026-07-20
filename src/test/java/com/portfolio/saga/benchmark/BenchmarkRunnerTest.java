package com.portfolio.saga.benchmark;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BenchmarkRunnerTest {

    @Test
    void shouldCompleteAllSagasWithZeroFailureProbability() {
        BenchmarkRunner runner = new BenchmarkRunner(50, 0.0, 42);
        ConsistencyResult result = runner.run();

        assertEquals(1.0, result.getValue(), 0.001);
        assertEquals(50, (int) result.getDetails().get("completed"));
        assertEquals(0, (int) result.getDetails().get("compensated"));
        assertEquals(0, (int) result.getDetails().get("failed"));
    }

    @Test
    void shouldCompensateAllSagasWithFullFailureProbability() {
        BenchmarkRunner runner = new BenchmarkRunner(50, 1.0, 42);
        ConsistencyResult result = runner.run();

        assertEquals(1.0, result.getValue(), 0.001);
        assertEquals(0, (int) result.getDetails().get("completed"));
        assertEquals(50, (int) result.getDetails().get("compensated"));
    }

    @Test
    void shouldProduceDeterministicResults() {
        BenchmarkRunner runner1 = new BenchmarkRunner(100, 0.2, 42);
        BenchmarkRunner runner2 = new BenchmarkRunner(100, 0.2, 42);

        ConsistencyResult r1 = runner1.run();
        ConsistencyResult r2 = runner2.run();

        assertEquals(r1.getValue(), r2.getValue(), 0.001);
        assertEquals(r1.getDetails().get("completed"), r2.getDetails().get("completed"));
        assertEquals(r1.getDetails().get("compensated"), r2.getDetails().get("compensated"));
    }

    @Test
    void shouldIncludeAllMetricFields() {
        BenchmarkRunner runner = new BenchmarkRunner(10, 0.0, 42);
        ConsistencyResult result = runner.run();

        assertNotNull(result.getProject());
        assertNotNull(result.getMetric());
        assertNotNull(result.getUnit());
        assertNotNull(result.getTimestamp());
        assertNotNull(result.getEnvironment());
        assertNotNull(result.getCommand());
        assertNotNull(result.getDetails());
    }

    @Test
    void shouldOutputValidJson() {
        BenchmarkRunner runner = new BenchmarkRunner(10, 0.3, 1);
        ConsistencyResult result = runner.run();
        String json = result.toJson();

        assertTrue(json.startsWith("{"));
        assertTrue(json.endsWith("}"));
        assertTrue(json.contains("consistency_rate"));
        assertTrue(json.contains("saga-orchestrator"));
    }

    @Test
    void shouldHandleMixedResults() {
        BenchmarkRunner runner = new BenchmarkRunner(200, 0.15, 99);
        ConsistencyResult result = runner.run();

        int total = (int) result.getDetails().get("completed")
                + (int) result.getDetails().get("compensated")
                + (int) result.getDetails().get("failed");
        assertEquals(200, total);
        assertTrue(result.getValue() >= 0.0);
        assertTrue(result.getValue() <= 1.0);
    }
}
