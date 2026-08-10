package com.portfolio.saga.infrastructure.postgres

import com.portfolio.saga.domain.OrderStep
import com.portfolio.saga.domain.SagaStep
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

abstract class JdbcIdempotentOrderStep(
    final override val step: OrderStep,
    private val table: String,
    private val activeStatus: String,
    private val compensatedStatus: String,
    private val jdbc: JdbcTemplate,
    private val transactions: TransactionTemplate,
) : SagaStep {
    override fun execute(orderId: String) {
        transactions.executeWithoutResult {
            jdbc.update(
                """
                INSERT INTO $table (order_id, idempotency_key, status)
                VALUES (?, ?, ?)
                ON CONFLICT (order_id) DO NOTHING
                """.trimIndent(),
                orderId,
                "$orderId:${step.contractName}:execute:v1",
                activeStatus,
            )
            check(status(orderId) == activeStatus) {
                "${step.contractName} cannot execute from ${status(orderId)}"
            }
        }
    }

    override fun compensate(orderId: String) {
        transactions.executeWithoutResult {
            jdbc.update(
                "UPDATE $table SET status = ?, updated_at = now() WHERE order_id = ? AND status = ?",
                compensatedStatus,
                orderId,
                activeStatus,
            )
            check(status(orderId) == compensatedStatus) {
                "${step.contractName} has no committed operation to compensate"
            }
        }
    }

    private fun status(orderId: String): String? = jdbc.query(
        "SELECT status FROM $table WHERE order_id = ?",
        { result, _ -> result.getString("status") },
        orderId,
    ).firstOrNull()
}

@Component
class ReserveInventoryStep(jdbc: JdbcTemplate, transactions: TransactionTemplate) :
    JdbcIdempotentOrderStep(
        OrderStep.RESERVE_INVENTORY,
        "inventory_reservations",
        "RESERVED",
        "RELEASED",
        jdbc,
        transactions,
    )

@Component
class AuthorizePaymentStep(jdbc: JdbcTemplate, transactions: TransactionTemplate) :
    JdbcIdempotentOrderStep(
        OrderStep.AUTHORIZE_PAYMENT,
        "payment_authorizations",
        "AUTHORIZED",
        "REFUNDED",
        jdbc,
        transactions,
    )

@Component
class CreateShipmentStep(jdbc: JdbcTemplate, transactions: TransactionTemplate) :
    JdbcIdempotentOrderStep(
        OrderStep.CREATE_SHIPMENT,
        "shipments",
        "CREATED",
        "CANCELLED",
        jdbc,
        transactions,
    )
