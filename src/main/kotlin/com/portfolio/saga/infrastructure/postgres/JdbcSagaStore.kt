package com.portfolio.saga.infrastructure.postgres

import com.fasterxml.jackson.databind.ObjectMapper
import com.portfolio.saga.domain.OrderStep
import com.portfolio.saga.domain.SagaInstance
import com.portfolio.saga.domain.SagaStatus
import com.portfolio.saga.domain.SagaStore
import com.portfolio.saga.domain.SagaTransition
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import org.springframework.transaction.support.TransactionTemplate
import java.sql.ResultSet
import java.time.Instant
import java.util.UUID

@Repository
class JdbcSagaStore(
    private val jdbc: JdbcTemplate,
    private val transactions: TransactionTemplate,
    private val objectMapper: ObjectMapper,
) : SagaStore {
    private val sagaMapper = RowMapper { result: ResultSet, _: Int -> result.toSaga() }

    override fun create(orderId: String): SagaInstance = transactions.execute {
        val sagaId = UUID.randomUUID()
        val inserted = jdbc.update(
            """
            INSERT INTO saga_instances (id, order_id, status)
            VALUES (?, ?, 'PENDING')
            ON CONFLICT (order_id) DO NOTHING
            """.trimIndent(),
            sagaId,
            orderId,
        )
        val saga = requireNotNull(loadByOrderId(orderId, lock = true))
        if (inserted == 1) {
            appendTransition(saga, null, "SAGA_STARTED", SagaStatus.PENDING, mapOf("orderId" to orderId))
        }
        saga
    } ?: error("transaction did not return a saga")

    override fun get(sagaId: UUID): SagaInstance? = load(sagaId)

    override fun getByOrderId(orderId: String): SagaInstance? = loadByOrderId(orderId)

    override fun findRecoverable(limit: Int): List<SagaInstance> = jdbc.query(
        """
        SELECT * FROM saga_instances
        WHERE status IN ('PENDING', 'RUNNING', 'COMPENSATING')
           OR (status = 'FAILED' AND compensation_step IS NOT NULL)
        ORDER BY updated_at, id
        LIMIT ?
        """.trimIndent(),
        sagaMapper,
        limit,
    )

    override fun transitions(sagaId: UUID): List<SagaTransition> = jdbc.query(
        """
        SELECT sequence, event_id, saga_id, step_name, action, from_status, to_status, occurred_at
        FROM saga_transitions
        WHERE saga_id = ?
        ORDER BY sequence
        """.trimIndent(),
        { result, _ ->
            SagaTransition(
                sequence = result.getLong("sequence"),
                eventId = result.getObject("event_id", UUID::class.java),
                sagaId = result.getObject("saga_id", UUID::class.java),
                step = result.getString("step_name")?.let { name ->
                    OrderStep.entries.firstOrNull { it.contractName == name }
                },
                action = result.getString("action"),
                fromStatus = result.getString("from_status")?.let(SagaStatus::valueOf),
                toStatus = SagaStatus.valueOf(result.getString("to_status")),
                occurredAt = result.getTimestamp("occurred_at").toInstant(),
            )
        },
        sagaId,
    )

    override fun markRunning(sagaId: UUID) = mutate(sagaId) { current ->
        if (current.status != SagaStatus.PENDING) return@mutate
        jdbc.update(
            "UPDATE saga_instances SET status = 'RUNNING', version = version + 1, updated_at = now() WHERE id = ?",
            sagaId,
        )
        appendTransition(current, null, "EXECUTION_STARTED", SagaStatus.RUNNING)
    }

    override fun markStepExecuted(sagaId: UUID, step: OrderStep) = mutate(sagaId) { current ->
        if (current.status != SagaStatus.RUNNING || current.nextStep > step.position) return@mutate
        check(current.nextStep == step.position) {
            "cannot record ${step.contractName}; expected step index ${current.nextStep}"
        }
        jdbc.update(
            """
            UPDATE saga_instances
            SET next_step = ?, version = version + 1, updated_at = now()
            WHERE id = ?
            """.trimIndent(),
            step.position + 1,
            sagaId,
        )
        appendTransition(current, step, "STEP_EXECUTED", SagaStatus.RUNNING)
    }

    override fun beginCompensation(sagaId: UUID, failedStep: OrderStep, reason: String) = mutate(sagaId) { current ->
        if (current.status != SagaStatus.RUNNING) return@mutate
        jdbc.update(
            """
            UPDATE saga_instances
            SET status = 'COMPENSATING', compensation_step = ?, failure_reason = ?,
                version = version + 1, updated_at = now()
            WHERE id = ?
            """.trimIndent(),
            current.nextStep - 1,
            reason,
            sagaId,
        )
        appendTransition(
            current,
            failedStep,
            "STEP_FAILED",
            SagaStatus.COMPENSATING,
            mapOf("reason" to reason),
        )
    }

    override fun markStepCompensated(sagaId: UUID, step: OrderStep) = mutate(sagaId) { current ->
        if (current.status != SagaStatus.COMPENSATING || current.compensationStep != step.position) return@mutate
        jdbc.update(
            """
            UPDATE saga_instances
            SET compensation_step = ?, version = version + 1, updated_at = now()
            WHERE id = ?
            """.trimIndent(),
            step.position - 1,
            sagaId,
        )
        appendTransition(current, step, "STEP_COMPENSATED", SagaStatus.COMPENSATING)
    }

    override fun resumeCompensation(sagaId: UUID) = mutate(sagaId) { current ->
        if (current.status != SagaStatus.FAILED || current.compensationStep == null) return@mutate
        jdbc.update(
            "UPDATE saga_instances SET status = 'COMPENSATING', version = version + 1, updated_at = now() WHERE id = ?",
            sagaId,
        )
        appendTransition(current, null, "RECOVERY_RESUMED", SagaStatus.COMPENSATING)
    }

    override fun markCompleted(sagaId: UUID) = mutate(sagaId) { current ->
        if (current.status != SagaStatus.RUNNING || current.nextStep != OrderStep.entries.size) return@mutate
        jdbc.update(
            "UPDATE saga_instances SET status = 'COMPLETED', version = version + 1, updated_at = now() WHERE id = ?",
            sagaId,
        )
        appendTransition(current, null, "SAGA_COMPLETED", SagaStatus.COMPLETED)
    }

    override fun markCompensated(sagaId: UUID) = mutate(sagaId) { current ->
        if (current.status != SagaStatus.COMPENSATING || (current.compensationStep ?: -1) >= 0) return@mutate
        jdbc.update(
            "UPDATE saga_instances SET status = 'COMPENSATED', version = version + 1, updated_at = now() WHERE id = ?",
            sagaId,
        )
        appendTransition(current, null, "SAGA_COMPENSATED", SagaStatus.COMPENSATED)
    }

    override fun markFailed(sagaId: UUID, step: OrderStep, reason: String) = mutate(sagaId) { current ->
        if (current.status != SagaStatus.COMPENSATING) return@mutate
        jdbc.update(
            """
            UPDATE saga_instances
            SET status = 'FAILED', compensation_step = ?, failure_reason = ?,
                version = version + 1, updated_at = now()
            WHERE id = ?
            """.trimIndent(),
            step.position,
            reason,
            sagaId,
        )
        appendTransition(
            current,
            step,
            "COMPENSATION_FAILED",
            SagaStatus.FAILED,
            mapOf("reason" to reason),
        )
    }

    private fun mutate(sagaId: UUID, block: (SagaInstance) -> Unit) {
        transactions.executeWithoutResult {
            block(requireNotNull(load(sagaId, lock = true)) { "saga $sagaId was not found" })
        }
    }

    private fun load(sagaId: UUID, lock: Boolean = false): SagaInstance? = jdbc.query(
        "SELECT * FROM saga_instances WHERE id = ?${if (lock) " FOR UPDATE" else ""}",
        sagaMapper,
        sagaId,
    ).firstOrNull()

    private fun loadByOrderId(orderId: String, lock: Boolean = false): SagaInstance? = jdbc.query(
        "SELECT * FROM saga_instances WHERE order_id = ?${if (lock) " FOR UPDATE" else ""}",
        sagaMapper,
        orderId,
    ).firstOrNull()

    private fun appendTransition(
        current: SagaInstance,
        step: OrderStep?,
        action: String,
        target: SagaStatus,
        payload: Map<String, Any?> = emptyMap(),
    ) {
        val eventId = UUID.randomUUID()
        val causationId = jdbc.query(
            "SELECT event_id FROM saga_transitions WHERE saga_id = ? ORDER BY sequence DESC LIMIT 1",
            { result, _ -> result.getObject("event_id", UUID::class.java) },
            current.id,
        ).firstOrNull() ?: eventId
        jdbc.update(
            """
            INSERT INTO saga_transitions (
                event_id, saga_id, event_type, aggregate_id, correlation_id, causation_id,
                step_name, action, from_status, to_status, payload
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
            ON CONFLICT (saga_id, step_name, action) DO NOTHING
            """.trimIndent(),
            eventId,
            current.id,
            "saga.$action",
            current.orderId,
            current.id,
            causationId,
            step?.contractName ?: "__saga__",
            action,
            current.status.name,
            target.name,
            objectMapper.writeValueAsString(payload),
        )
    }

    private fun ResultSet.toSaga() = SagaInstance(
        id = getObject("id", UUID::class.java),
        orderId = getString("order_id"),
        status = SagaStatus.valueOf(getString("status")),
        nextStep = getInt("next_step"),
        compensationStep = getObject("compensation_step")?.let { getInt("compensation_step") },
        failureReason = getString("failure_reason"),
        version = getLong("version"),
        createdAt = getTimestamp("created_at").toInstant(),
        updatedAt = getTimestamp("updated_at").toInstant(),
    )
}
