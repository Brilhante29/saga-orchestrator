package com.portfolio.saga.domain;

import com.portfolio.saga.application.InMemorySagaLog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderSagaTest {

    @Test
    void shouldCompleteOrderSagaSuccessfully() {
        InMemorySagaLog log = new InMemorySagaLog();
        Saga saga = new OrderSaga(log, "order-123");
        SagaOrchestrator orchestrator = new SagaOrchestrator();
        SagaStatus result = orchestrator.execute(saga);

        assertEquals(SagaStatus.COMPLETED, result);
    }

    @Test
    void shouldCompensateWhenPaymentFails() {
        InMemorySagaLog log = new InMemorySagaLog();
        Saga saga = new OrderSaga(log, "order-456", false, true, false);
        SagaOrchestrator orchestrator = new SagaOrchestrator();
        SagaStatus result = orchestrator.execute(saga);

        assertEquals(SagaStatus.COMPENSATED, result);
    }

    @Test
    void shouldCompensateWhenShippingFails() {
        InMemorySagaLog log = new InMemorySagaLog();
        Saga saga = new OrderSaga(log, "order-789", false, false, true);
        SagaOrchestrator orchestrator = new SagaOrchestrator();
        SagaStatus result = orchestrator.execute(saga);

        assertEquals(SagaStatus.COMPENSATED, result);
    }

    @Test
    void shouldCompensateWhenInventoryFails() {
        InMemorySagaLog log = new InMemorySagaLog();
        Saga saga = new OrderSaga(log, "order-000", true, false, false);
        SagaOrchestrator orchestrator = new SagaOrchestrator();
        SagaStatus result = orchestrator.execute(saga);

        assertEquals(SagaStatus.COMPENSATED, result);
    }

    @Test
    void shouldUseFailAtStepConstructor() {
        InMemorySagaLog log = new InMemorySagaLog();
        Saga saga = new OrderSaga(log, "order-xyz", 2);
        SagaOrchestrator orchestrator = new SagaOrchestrator();
        SagaStatus result = orchestrator.execute(saga);

        assertEquals(SagaStatus.COMPENSATED, result);
    }
}
