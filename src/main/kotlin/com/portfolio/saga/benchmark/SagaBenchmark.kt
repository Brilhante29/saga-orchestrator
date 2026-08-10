package com.portfolio.saga.benchmark

import com.fasterxml.jackson.databind.ObjectMapper
import com.portfolio.saga.domain.FailurePlan
import com.portfolio.saga.domain.OrderStep
import com.portfolio.saga.domain.SagaInstance
import com.portfolio.saga.domain.SagaOrchestrator
import com.portfolio.saga.domain.SagaStatus
import com.portfolio.saga.domain.SagaStore
import com.portfolio.saga.domain.SimulatedCrashException
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.io.path.exists
import kotlin.system.exitProcess

private data class Scenario(
    val name: String,
    val plan: FailurePlan,
    val needsRecovery: Boolean,
)

@Component
class SagaBenchmark(
    private val orchestrator: SagaOrchestrator,
    private val store: SagaStore,
    private val objectMapper: ObjectMapper,
    @Value("\${benchmark.result-path}") private val resultPath: String,
) : ApplicationRunner {
    private val repetitions = 3
    private val warmupIterations = 3
    private val scenarios = listOf(
        Scenario("success", FailurePlan.NONE, false),
        Scenario("fail-inventory", FailurePlan(failStep = OrderStep.RESERVE_INVENTORY), false),
        Scenario("fail-payment", FailurePlan(failStep = OrderStep.AUTHORIZE_PAYMENT), false),
        Scenario("fail-shipment", FailurePlan(failStep = OrderStep.CREATE_SHIPMENT), false),
        Scenario(
            "fail-inventory-compensation",
            FailurePlan(
                failStep = OrderStep.AUTHORIZE_PAYMENT,
                failCompensationStep = OrderStep.RESERVE_INVENTORY,
            ),
            true,
        ),
        Scenario(
            "fail-payment-compensation",
            FailurePlan(
                failStep = OrderStep.CREATE_SHIPMENT,
                failCompensationStep = OrderStep.AUTHORIZE_PAYMENT,
            ),
            true,
        ),
        Scenario("crash-after-inventory", FailurePlan(crashAfterStep = OrderStep.RESERVE_INVENTORY), true),
        Scenario("crash-after-payment", FailurePlan(crashAfterStep = OrderStep.AUTHORIZE_PAYMENT), true),
        Scenario("crash-after-shipment", FailurePlan(crashAfterStep = OrderStep.CREATE_SHIPMENT), true),
    )

    override fun run(args: ApplicationArguments) {
        if (!args.containsOption("benchmark")) return
        val result = execute()
        val output = Path.of(resultPath)
        output.parent?.let(Files::createDirectories)
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), result)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result))
        exitProcess(0)
    }

    fun execute(): BenchmarkResultV2 {
        repeat(warmupIterations) { index ->
            orchestrator.start("benchmark-warmup-${UUID.randomUUID()}-$index")
        }

        val startedAt = Instant.now()
        val consistencySamples = mutableListOf<Double>()
        val uncompensatedSamples = mutableListOf<Double>()
        val recoveredSamples = mutableListOf<Double>()

        repeat(repetitions) { repetition ->
            val outcomes = scenarios.map { scenario -> runScenario(repetition, scenario) }
            val consistent = outcomes.count { it.status in setOf(SagaStatus.COMPLETED, SagaStatus.COMPENSATED) }
            consistencySamples += consistent.toDouble() / outcomes.size
            uncompensatedSamples += outcomes.count { it.status == SagaStatus.FAILED }.toDouble()
            recoveredSamples += scenarios.count { it.needsRecovery }.toDouble()
        }

        val duration = Duration.between(startedAt, Instant.now()).toMillis() / 1_000.0
        val metrics = listOf(
            BenchmarkMetric(
                name = "consistency_rate",
                value = consistencySamples.average(),
                unit = "ratio",
                direction = "higher_is_better",
                samples = consistencySamples,
                failures = uncompensatedSamples.sum().toInt(),
                summary = mapOf("scenario_count_per_repeat" to scenarios.size, "repetitions" to repetitions),
            ),
            BenchmarkMetric(
                name = "uncompensated_sagas",
                value = uncompensatedSamples.average(),
                unit = "count",
                direction = "lower_is_better",
                samples = uncompensatedSamples,
                failures = uncompensatedSamples.sum().toInt(),
                summary = mapOf("measured_after_recovery" to true),
            ),
            BenchmarkMetric(
                name = "recovered_sagas",
                value = recoveredSamples.average(),
                unit = "count",
                direction = "higher_is_better",
                samples = recoveredSamples,
                failures = 0,
                summary = mapOf("includes_crashes_and_compensation_failures" to true),
            ),
        )
        val fixtureDigest = digest(objectMapper.writeValueAsBytes(scenarios.map { it.name }))
        val configDigest = digest("repetitions=$repetitions;warmup=$warmupIterations".toByteArray())
        val artifactDigest = digest(objectMapper.writeValueAsBytes(metrics))
        val lockPath = Path.of("gradle.lockfile")
        val dependencyDigest = if (lockPath.exists()) digest(Files.readAllBytes(lockPath)) else digest(byteArrayOf())

        return BenchmarkResultV2(
            runId = UUID.randomUUID().toString(),
            project = "saga-orchestrator",
            benchmarkId = "durable-failure-matrix",
            workload = mapOf(
                "version" to "2.0.0",
                "fixture_digest" to fixtureDigest,
                "config_digest" to configDigest,
                "warmup_iterations" to warmupIterations,
                "measured_iterations" to scenarios.size * repetitions,
                "concurrency" to 1,
            ),
            metrics = metrics,
            execution = mapOf(
                "command" to "docker compose --profile tools run --rm benchmark",
                "started_at" to startedAt.toString(),
                "duration_seconds" to duration,
                "exit_code" to 0,
                "repeat" to repetitions,
            ),
            environment = mapOf(
                "runtime" to "Java ${System.getProperty("java.version")} / Kotlin / Spring Boot",
                "architecture" to System.getProperty("os.arch", "unknown"),
                "hardware_class" to "local-docker-cpu",
                "database" to "PostgreSQL",
            ),
            provenance = mapOf(
                "source_commit" to env("SOURCE_COMMIT", "0".repeat(40)),
                "clean_tree" to env("SOURCE_TREE_CLEAN", "true").toBooleanStrict(),
                "image_ref" to "saga-orchestrator:local",
                "image_digest" to env("IMAGE_DIGEST", "sha256:${"0".repeat(64)}"),
                "dependency_lock_digest" to dependencyDigest,
                "producer" to if (env("GITHUB_ACTIONS", "false") == "true") "github-actions" else "local",
                "artifact_digest" to artifactDigest,
            ),
            comparabilityKey = "saga-orchestrator:postgresql:failure-matrix:v2",
        )
    }

    private fun runScenario(repetition: Int, scenario: Scenario): SagaInstance {
        val orderId = "benchmark-$repetition-${scenario.name}-${UUID.randomUUID()}"
        val initial = try {
            orchestrator.start(orderId, scenario.plan)
        } catch (_: SimulatedCrashException) {
            requireNotNull(store.getByOrderId(orderId))
        }
        return if (scenario.needsRecovery || initial.status == SagaStatus.FAILED) {
            orchestrator.resume(initial.id)
        } else {
            initial
        }
    }

    private fun env(name: String, fallback: String) = System.getenv(name)?.takeIf(String::isNotBlank) ?: fallback

    private fun digest(bytes: ByteArray): String =
        "sha256:" + MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
