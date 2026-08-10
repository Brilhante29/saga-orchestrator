package com.portfolio.saga.domain

import java.time.Instant
import java.util.UUID

class InMemorySagaStore : SagaStore {
    private val sagas = linkedMapOf<UUID, SagaInstance>()
    private val byOrder = mutableMapOf<String, UUID>()

    override fun create(orderId: String): SagaInstance {
        byOrder[orderId]?.let { return requireNotNull(sagas[it]) }
        val now = Instant.now()
        return SagaInstance(UUID.randomUUID(), orderId, SagaStatus.PENDING, 0, null, null, 0, now, now)
            .also { sagas[it.id] = it; byOrder[orderId] = it.id }
    }

    override fun get(sagaId: UUID) = sagas[sagaId]
    override fun getByOrderId(orderId: String) = byOrder[orderId]?.let(sagas::get)
    override fun findRecoverable(limit: Int) = sagas.values.filter {
        it.status in setOf(SagaStatus.PENDING, SagaStatus.RUNNING, SagaStatus.COMPENSATING) ||
            (it.status == SagaStatus.FAILED && it.compensationStep != null)
    }.take(limit)
    override fun transitions(sagaId: UUID) = emptyList<SagaTransition>()

    override fun markRunning(sagaId: UUID) = update(sagaId) { it.copy(status = SagaStatus.RUNNING) }
    override fun markStepExecuted(sagaId: UUID, step: OrderStep) =
        update(sagaId) { it.copy(nextStep = maxOf(it.nextStep, step.position + 1)) }
    override fun beginCompensation(sagaId: UUID, failedStep: OrderStep, reason: String) =
        update(sagaId) {
            it.copy(status = SagaStatus.COMPENSATING, compensationStep = it.nextStep - 1, failureReason = reason)
        }
    override fun markStepCompensated(sagaId: UUID, step: OrderStep) =
        update(sagaId) { it.copy(compensationStep = step.position - 1) }
    override fun resumeCompensation(sagaId: UUID) =
        update(sagaId) { it.copy(status = SagaStatus.COMPENSATING) }
    override fun markCompleted(sagaId: UUID) =
        update(sagaId) { it.copy(status = SagaStatus.COMPLETED) }
    override fun markCompensated(sagaId: UUID) =
        update(sagaId) { it.copy(status = SagaStatus.COMPENSATED) }
    override fun markFailed(sagaId: UUID, step: OrderStep, reason: String) =
        update(sagaId) { it.copy(status = SagaStatus.FAILED, compensationStep = step.position, failureReason = reason) }

    private fun update(id: UUID, mutation: (SagaInstance) -> SagaInstance) {
        val current = requireNotNull(sagas[id])
        sagas[id] = mutation(current).copy(version = current.version + 1, updatedAt = Instant.now())
    }
}

class IdempotentTestStep(override val step: OrderStep) : SagaStep {
    val activeOrders = mutableSetOf<String>()
    val compensatedOrders = mutableSetOf<String>()
    var executionAttempts = 0
    var compensationAttempts = 0

    override fun execute(orderId: String) {
        executionAttempts++
        activeOrders += orderId
    }

    override fun compensate(orderId: String) {
        compensationAttempts++
        check(orderId in activeOrders)
        activeOrders -= orderId
        compensatedOrders += orderId
    }
}
