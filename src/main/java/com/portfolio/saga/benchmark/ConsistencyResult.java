package com.portfolio.saga.benchmark;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;

public class ConsistencyResult {
    private final String project;
    private final String metric;
    private final double value;
    private final String unit;
    private final String timestamp;
    private final Map<String, Object> environment;
    private final String command;
    private final Map<String, Object> details;

    public ConsistencyResult(String project, String metric, double value, String unit,
                             Map<String, Object> environment, String command,
                             Map<String, Object> details) {
        this.project = project;
        this.metric = metric;
        this.value = value;
        this.unit = unit;
        this.timestamp = Instant.now().toString();
        this.environment = environment;
        this.command = command;
        this.details = details;
    }

    @JsonProperty("project")
    public String getProject() { return project; }

    @JsonProperty("metric")
    public String getMetric() { return metric; }

    @JsonProperty("value")
    public double getValue() { return value; }

    @JsonProperty("unit")
    public String getUnit() { return unit; }

    @JsonProperty("timestamp")
    public String getTimestamp() { return timestamp; }

    @JsonProperty("environment")
    public Map<String, Object> getEnvironment() { return environment; }

    @JsonProperty("command")
    public String getCommand() { return command; }

    @JsonProperty("details")
    public Map<String, Object> getDetails() { return details; }

    public String toJson() {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(this);
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}
