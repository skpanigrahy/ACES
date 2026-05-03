package com.aces.common.model;

import java.time.Instant;

public class EvaluationResult {

    private String correlationId;
    private boolean driftDetected;
    private String driftType;
    private String severity;
    private String reasoning;
    private Instant evaluatedAt;

    public EvaluationResult() {
    }

    public EvaluationResult(String correlationId, boolean driftDetected, String driftType, String severity, String reasoning, Instant evaluatedAt) {
        this.correlationId = correlationId;
        this.driftDetected = driftDetected;
        this.driftType = driftType;
        this.severity = severity;
        this.reasoning = reasoning;
        this.evaluatedAt = evaluatedAt;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public boolean isDriftDetected() {
        return driftDetected;
    }

    public void setDriftDetected(boolean driftDetected) {
        this.driftDetected = driftDetected;
    }

    public String getDriftType() {
        return driftType;
    }

    public void setDriftType(String driftType) {
        this.driftType = driftType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(Instant evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }
}
