package com.portfolio.saga.benchmark

import com.fasterxml.jackson.annotation.JsonProperty

data class BenchmarkResultV2(
    @JsonProperty("schema_version") val schemaVersion: Int = 2,
    @JsonProperty("run_id") val runId: String,
    val project: String,
    @JsonProperty("benchmark_id") val benchmarkId: String,
    val workload: Map<String, Any>,
    val metrics: List<BenchmarkMetric>,
    val execution: Map<String, Any>,
    val environment: Map<String, Any>,
    val provenance: Map<String, Any>,
    @JsonProperty("comparability_key") val comparabilityKey: String,
)

data class BenchmarkMetric(
    val name: String,
    val value: Double,
    val unit: String,
    val direction: String,
    val samples: List<Double>,
    val failures: Int,
    val summary: Map<String, Any>,
)
