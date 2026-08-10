package com.portfolio.saga.application

import com.portfolio.saga.domain.FailurePlan
import com.portfolio.saga.domain.OrderStep
import com.portfolio.saga.domain.SagaInstance
import com.portfolio.saga.domain.SagaOrchestrator
import com.portfolio.saga.domain.SagaStore
import com.portfolio.saga.domain.SagaTransition
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

data class StartSagaRequest(
    val orderId: String,
    val failStep: OrderStep? = null,
    val failCompensationStep: OrderStep? = null,
    val crashAfterStep: OrderStep? = null,
)

data class SagaResponse(
    val saga: SagaInstance,
    val transitions: List<SagaTransition>,
)

@RestController
@RequestMapping("/api/sagas")
class SagaController(
    private val orchestrator: SagaOrchestrator,
    private val store: SagaStore,
) {
    @PostMapping
    fun start(@RequestBody request: StartSagaRequest): ResponseEntity<SagaResponse> {
        val result = orchestrator.start(
            request.orderId,
            FailurePlan(request.failStep, request.failCompensationStep, request.crashAfterStep),
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(result.toResponse())
    }

    @PostMapping("/{sagaId}/recover")
    fun recover(@PathVariable sagaId: UUID): SagaResponse = orchestrator.resume(sagaId).toResponse()

    @PostMapping("/recover")
    fun recoverAll(): List<SagaResponse> = orchestrator.recover().map { it.toResponse() }

    @GetMapping("/{sagaId}")
    fun get(@PathVariable sagaId: UUID): SagaResponse =
        (store.get(sagaId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)).toResponse()

    private fun SagaInstance.toResponse() = SagaResponse(this, store.transitions(id))
}
