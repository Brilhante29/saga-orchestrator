package com.portfolio.saga.domain

import java.util.UUID

class SagaOrchestrator(
    private val store: SagaStore,
    steps: List<SagaStep>,
) {
    private val steps = steps.sortedBy { it.step.position }

    init {
        require(this.steps.map { it.step } == OrderStep.entries) {
            "exactly one implementation is required for every order saga step"
        }
    }

    fun start(orderId: String, failurePlan: FailurePlan = FailurePlan.NONE): SagaInstance {
        require(orderId.isNotBlank()) { "orderId must not be blank" }
        val saga = store.create(orderId)
        return resume(saga.id, failurePlan)
    }

    fun resume(sagaId: UUID, failurePlan: FailurePlan = FailurePlan.NONE): SagaInstance {
        var saga = requireNotNull(store.get(sagaId)) { "saga $sagaId was not found" }

        if (saga.status == SagaStatus.PENDING) {
            store.markRunning(sagaId)
            saga = requireNotNull(store.get(sagaId))
        }

        if (saga.status == SagaStatus.FAILED && saga.compensationStep != null) {
            store.resumeCompensation(sagaId)
            saga = requireNotNull(store.get(sagaId))
        }

        return when (saga.status) {
            SagaStatus.RUNNING -> executeForward(saga, failurePlan)
            SagaStatus.COMPENSATING -> compensate(saga, failurePlan)
            else -> saga
        }
    }

    fun recover(limit: Int = 100): List<SagaInstance> =
        store.findRecoverable(limit).map { resume(it.id) }

    private fun executeForward(initial: SagaInstance, failurePlan: FailurePlan): SagaInstance {
        for (index in initial.nextStep until steps.size) {
            val current = steps[index]
            try {
                if (failurePlan.failStep == current.step) {
                    error("injected execution failure at ${current.step.contractName}")
                }
                current.execute(initial.orderId)
                if (failurePlan.crashAfterStep == current.step) {
                    throw SimulatedCrashException(current.step)
                }
                store.markStepExecuted(initial.id, current.step)
            } catch (crash: SimulatedCrashException) {
                throw crash
            } catch (failure: Exception) {
                store.beginCompensation(
                    initial.id,
                    current.step,
                    failure.message ?: failure::class.simpleName.orEmpty(),
                )
                return compensate(requireNotNull(store.get(initial.id)), failurePlan)
            }
        }

        store.markCompleted(initial.id)
        return requireNotNull(store.get(initial.id))
    }

    private fun compensate(initial: SagaInstance, failurePlan: FailurePlan): SagaInstance {
        val first = initial.compensationStep ?: -1
        for (index in first downTo 0) {
            val current = steps[index]
            try {
                if (failurePlan.failCompensationStep == current.step) {
                    error("injected compensation failure at ${current.step.contractName}")
                }
                current.compensate(initial.orderId)
                store.markStepCompensated(initial.id, current.step)
            } catch (failure: Exception) {
                store.markFailed(
                    initial.id,
                    current.step,
                    failure.message ?: failure::class.simpleName.orEmpty(),
                )
                return requireNotNull(store.get(initial.id))
            }
        }

        store.markCompensated(initial.id)
        return requireNotNull(store.get(initial.id))
    }
}
