package com.portfolio.saga.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SagaOrchestratorTest {
    private val store = InMemorySagaStore()
    private val steps = OrderStep.entries.map(::IdempotentTestStep)
    private val orchestrator = SagaOrchestrator(store, steps)

    @Test
    fun `completes every operation exactly once`() {
        val result = orchestrator.start("order-complete")

        assertEquals(SagaStatus.COMPLETED, result.status)
        assertEquals(3, result.nextStep)
        steps.forEach { assertEquals(setOf("order-complete"), it.activeOrders) }
    }

    @Test
    fun `failure at every step reaches a consistent compensated state`() {
        OrderStep.entries.forEach { failedStep ->
            val orderId = "order-fail-${failedStep.position}"
            val result = orchestrator.start(orderId, FailurePlan(failStep = failedStep))

            assertEquals(SagaStatus.COMPENSATED, result.status)
            steps.take(failedStep.position).forEach { assertEquals(true, orderId in it.compensatedOrders) }
            steps.drop(failedStep.position).forEach { assertEquals(false, orderId in it.activeOrders) }
        }
    }

    @Test
    fun `compensation failure is FAILED until recovery succeeds`() {
        val failed = orchestrator.start(
            "order-compensation-failure",
            FailurePlan(
                failStep = OrderStep.CREATE_SHIPMENT,
                failCompensationStep = OrderStep.AUTHORIZE_PAYMENT,
            ),
        )

        assertEquals(SagaStatus.FAILED, failed.status)
        assertEquals(OrderStep.AUTHORIZE_PAYMENT.position, failed.compensationStep)

        val recovered = orchestrator.resume(failed.id)
        assertEquals(SagaStatus.COMPENSATED, recovered.status)
        assertEquals(emptySet<String>(), steps[0].activeOrders)
        assertEquals(emptySet<String>(), steps[1].activeOrders)
    }

    @Test
    fun `crash after side effect resumes idempotently from durable index`() {
        assertThrows(SimulatedCrashException::class.java) {
            orchestrator.start(
                "order-crash",
                FailurePlan(crashAfterStep = OrderStep.AUTHORIZE_PAYMENT),
            )
        }
        val interrupted = requireNotNull(store.getByOrderId("order-crash"))
        assertEquals(SagaStatus.RUNNING, interrupted.status)
        assertEquals(1, interrupted.nextStep)

        val recovered = orchestrator.resume(interrupted.id)
        assertEquals(SagaStatus.COMPLETED, recovered.status)
        assertEquals(2, steps[1].executionAttempts)
        assertEquals(setOf("order-crash"), steps[1].activeOrders)
    }

    @Test
    fun `order id is the idempotency boundary`() {
        val first = orchestrator.start("same-order")
        val second = orchestrator.start("same-order")

        assertEquals(first.id, second.id)
        assertSame(store.get(first.id), store.get(second.id))
        steps.forEach { assertEquals(1, it.executionAttempts) }
    }
}
