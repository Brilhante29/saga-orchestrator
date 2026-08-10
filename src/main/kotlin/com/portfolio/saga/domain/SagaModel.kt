package com.portfolio.saga.domain

import java.time.Instant
import java.util.UUID

enum class SagaStatus {
    PENDING,
    RUNNING,
    COMPENSATING,
    COMPLETED,
    COMPENSATED,
    FAILED,
}

enum class OrderStep(val position: Int, val contractName: String) {
    RESERVE_INVENTORY(0, "reserve-inventory"),
    AUTHORIZE_PAYMENT(1, "authorize-payment"),
    CREATE_SHIPMENT(2, "create-shipment"),
}

data class SagaInstance(
    val id: UUID,
    val orderId: String,
    val status: SagaStatus,
    val nextStep: Int,
    val compensationStep: Int?,
    val failureReason: String?,
    val version: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class SagaTransition(
    val sequence: Long,
    val eventId: UUID,
    val sagaId: UUID,
    val step: OrderStep?,
    val action: String,
    val fromStatus: SagaStatus?,
    val toStatus: SagaStatus,
    val occurredAt: Instant,
)

data class FailurePlan(
    val failStep: OrderStep? = null,
    val failCompensationStep: OrderStep? = null,
    val crashAfterStep: OrderStep? = null,
) {
    companion object {
        val NONE = FailurePlan()
    }
}

class SimulatedCrashException(step: OrderStep) :
    RuntimeException("simulated process crash after ${step.contractName}")
