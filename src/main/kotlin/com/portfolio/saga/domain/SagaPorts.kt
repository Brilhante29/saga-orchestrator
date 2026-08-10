package com.portfolio.saga.domain

import java.util.UUID

interface SagaStep {
    val step: OrderStep

    /** Execute and compensate must be idempotent for the same orderId. */
    fun execute(orderId: String)

    fun compensate(orderId: String)
}

interface SagaStore {
    fun create(orderId: String): SagaInstance
    fun get(sagaId: UUID): SagaInstance?
    fun getByOrderId(orderId: String): SagaInstance?
    fun findRecoverable(limit: Int = 100): List<SagaInstance>
    fun transitions(sagaId: UUID): List<SagaTransition>
    fun markRunning(sagaId: UUID)
    fun markStepExecuted(sagaId: UUID, step: OrderStep)
    fun beginCompensation(sagaId: UUID, failedStep: OrderStep, reason: String)
    fun markStepCompensated(sagaId: UUID, step: OrderStep)
    fun resumeCompensation(sagaId: UUID)
    fun markCompleted(sagaId: UUID)
    fun markCompensated(sagaId: UUID)
    fun markFailed(sagaId: UUID, step: OrderStep, reason: String)
}
