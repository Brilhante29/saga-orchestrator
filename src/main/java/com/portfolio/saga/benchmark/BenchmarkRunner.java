package com.portfolio.saga.benchmark;

import com.portfolio.saga.application.InMemorySagaLog;
import com.portfolio.saga.domain.Saga;
import com.portfolio.saga.domain.SagaOrchestrator;
import com.portfolio.saga.domain.SagaStatus;

import java.util.*;

public class BenchmarkRunner {
    private final int iterations;
    private final double failureProbability;
    private final long seed;

    public BenchmarkRunner(int iterations, double failureProbability, long seed) {
        this.iterations = iterations;
        this.failureProbability = failureProbability;
        this.seed = seed;
    }

    public ConsistencyResult run() {
        SagaOrchestrator orchestrator = new SagaOrchestrator();
        Random rng = new Random(seed);

        int completed = 0;
        int compensated = 0;
        int failed = 0;

        for (int i = 0; i < iterations; i++) {
            InMemorySagaLog log = new InMemorySagaLog();
            String orderId = "bench-order-" + i;
            boolean failInventory = rng.nextDouble() < failureProbability;
            boolean failPayment = !failInventory && rng.nextDouble() < failureProbability;
            boolean failShip = !failPayment && !failInventory && rng.nextDouble() < failureProbability;

            Saga saga = new Saga(
                    List.of(
                            failInventory ? new FailingStep("reserve-inventory") : new SuccessfulStep("reserve-inventory"),
                            failPayment ? new FailingStep("process-payment") : new SuccessfulStep("process-payment"),
                            failShip ? new FailingStep("ship-order") : new SuccessfulStep("ship-order")
                    ),
                    log, orderId
            );

            SagaStatus result = orchestrator.execute(saga);
            switch (result) {
                case COMPLETED -> completed++;
                case COMPENSATED -> compensated++;
                default -> failed++;
            }
        }

        double consistencyRate = (double) (completed + compensated) / iterations;

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("iterations", iterations);
        details.put("failure_probability", failureProbability);
        details.put("seed", seed);
        details.put("completed", completed);
        details.put("compensated", compensated);
        details.put("failed", failed);

        Map<String, Object> env = new LinkedHashMap<>();
        env.put("java_version", System.getProperty("java.version", "unknown"));
        env.put("os", System.getProperty("os.name", "unknown"));
        env.put("available_processors", Runtime.getRuntime().availableProcessors());

        return new ConsistencyResult(
                "saga-orchestrator",
                "consistency_rate",
                consistencyRate,
                "unit",
                env,
                "java -jar saga-orchestrator.jar benchmark",
                details
        );
    }

    private static class SuccessfulStep implements com.portfolio.saga.domain.SagaStep<String, String> {
        private final String name;
        SuccessfulStep(String name) { this.name = name; }
        @Override public String getName() { return name; }
        @Override public String execute(String input) { return name + " ok for " + input; }
        @Override public void compensate(String input) { }
    }

    private static class FailingStep implements com.portfolio.saga.domain.SagaStep<String, String> {
        private final String name;
        FailingStep(String name) { this.name = name; }
        @Override public String getName() { return name; }
        @Override public String execute(String input) { throw new RuntimeException(name + " failed for " + input); }
        @Override public void compensate(String input) { }
    }
}
