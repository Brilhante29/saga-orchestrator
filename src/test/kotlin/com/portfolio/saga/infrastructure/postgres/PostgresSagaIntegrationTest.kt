package com.portfolio.saga.infrastructure.postgres

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.portfolio.saga.domain.FailurePlan
import com.portfolio.saga.domain.OrderStep
import com.portfolio.saga.domain.SagaOrchestrator
import com.portfolio.saga.domain.SagaStatus
import com.portfolio.saga.domain.SimulatedCrashException
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.containers.PostgreSQLContainer

class PostgresSagaIntegrationTest {
    companion object {
        private val externalUrl = System.getenv("TEST_DATABASE_URL")
        private val postgres = PostgreSQLContainer<Nothing>("postgres:17.6-alpine")

        @BeforeAll
        @JvmStatic
        fun startDatabase() {
            if (externalUrl.isNullOrBlank()) postgres.start()
        }

        @AfterAll
        @JvmStatic
        fun stopDatabase() {
            if (externalUrl.isNullOrBlank()) postgres.stop()
        }
    }

    private lateinit var jdbc: JdbcTemplate
    private lateinit var store: JdbcSagaStore
    private lateinit var orchestrator: SagaOrchestrator

    @BeforeEach
    fun setUp() {
        val dataSource = if (externalUrl.isNullOrBlank()) {
            DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password)
        } else {
            DriverManagerDataSource(externalUrl, "saga", "saga")
        }
        Flyway.configure().dataSource(dataSource).load().migrate()
        jdbc = JdbcTemplate(dataSource)
        jdbc.execute(
            "TRUNCATE saga_transitions, saga_instances, inventory_reservations, payment_authorizations, shipments CASCADE",
        )
        val transactions = TransactionTemplate(DataSourceTransactionManager(dataSource))
        store = JdbcSagaStore(jdbc, transactions, jacksonObjectMapper().findAndRegisterModules())
        val steps = listOf(
            ReserveInventoryStep(jdbc, transactions),
            AuthorizePaymentStep(jdbc, transactions),
            CreateShipmentStep(jdbc, transactions),
        )
        orchestrator = SagaOrchestrator(store, steps)
    }

    @Test
    fun `restart after committed payment replays without duplicate side effect`() {
        assertThrows(SimulatedCrashException::class.java) {
            orchestrator.start("postgres-crash", FailurePlan(crashAfterStep = OrderStep.AUTHORIZE_PAYMENT))
        }
        val interrupted = requireNotNull(store.getByOrderId("postgres-crash"))
        assertEquals(SagaStatus.RUNNING, interrupted.status)
        assertEquals(1, interrupted.nextStep)

        val recovered = orchestrator.resume(interrupted.id)
        assertEquals(SagaStatus.COMPLETED, recovered.status)
        assertEquals(1, count("inventory_reservations", "postgres-crash"))
        assertEquals(1, count("payment_authorizations", "postgres-crash"))
        assertEquals(1, count("shipments", "postgres-crash"))
    }

    @Test
    fun `failed compensation is durable and a new orchestrator recovers it`() {
        val failed = orchestrator.start(
            "postgres-compensation",
            FailurePlan(
                failStep = OrderStep.CREATE_SHIPMENT,
                failCompensationStep = OrderStep.AUTHORIZE_PAYMENT,
            ),
        )
        assertEquals(SagaStatus.FAILED, failed.status)
        assertEquals("AUTHORIZED", status("payment_authorizations", failed.orderId))

        val restarted = SagaOrchestrator(
            JdbcSagaStore(jdbc, transactionTemplate(), jacksonObjectMapper()),
            listOf(
                ReserveInventoryStep(jdbc, transactionTemplate()),
                AuthorizePaymentStep(jdbc, transactionTemplate()),
                CreateShipmentStep(jdbc, transactionTemplate()),
            ),
        )
        val recovered = restarted.resume(failed.id)

        assertEquals(SagaStatus.COMPENSATED, recovered.status)
        assertEquals("RELEASED", status("inventory_reservations", failed.orderId))
        assertEquals("REFUNDED", status("payment_authorizations", failed.orderId))
        assertEquals(
            listOf("COMPENSATION_FAILED", "RECOVERY_RESUMED"),
            store.transitions(failed.id).map { it.action }.filter { it in setOf("COMPENSATION_FAILED", "RECOVERY_RESUMED") },
        )
    }

    @Test
    fun `starting the same order twice preserves one saga and one operation row`() {
        val first = orchestrator.start("postgres-idempotent")
        val second = orchestrator.start("postgres-idempotent")

        assertEquals(first.id, second.id)
        assertEquals(1, count("saga_instances", "postgres-idempotent"))
        assertEquals(1, count("inventory_reservations", "postgres-idempotent"))
        assertEquals(1, count("payment_authorizations", "postgres-idempotent"))
        assertEquals(1, count("shipments", "postgres-idempotent"))
    }

    private fun transactionTemplate() = TransactionTemplate(DataSourceTransactionManager(jdbc.dataSource!!))

    private fun count(table: String, orderId: String): Int =
        requireNotNull(jdbc.queryForObject("SELECT count(*) FROM $table WHERE order_id = ?", Int::class.java, orderId))

    private fun status(table: String, orderId: String): String =
        requireNotNull(jdbc.queryForObject("SELECT status FROM $table WHERE order_id = ?", String::class.java, orderId))
}
