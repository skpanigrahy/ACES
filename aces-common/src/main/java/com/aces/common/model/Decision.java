package com.aces.common.model;

import java.time.Instant;

public class Decision {

    private String correlationId;
    private boolean actionRequired;
    private String strategy;
    private String skill;
    private String reasoning;
    private double confidenceScore;
    private boolean policyApproved;
    private String approvalType;
    private Instant decidedAt;

    public Decision() {
    }

    private Decision(DecisionBuilder builder) {
        this.correlationId = builder.correlationId;
        this.actionRequired = builder.actionRequired;
        this.strategy = builder.strategy;
        this.skill = builder.skill;
        this.reasoning = builder.reasoning;
        this.confidenceScore = builder.confidenceScore;
        this.policyApproved = builder.policyApproved;
        this.approvalType = builder.approvalType;
        this.decidedAt = builder.decidedAt;
    }

    public static DecisionBuilder builder() {
        return new DecisionBuilder();
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public boolean isActionRequired() {
        return actionRequired;
    }

    public void setActionRequired(boolean actionRequired) {
        this.actionRequired = actionRequired;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public boolean isPolicyApproved() {
        return policyApproved;
    }

    public void setPolicyApproved(boolean policyApproved) {
        this.policyApproved = policyApproved;
    }

    public String getApprovalType() {
        return approvalType;
    }

    public void setApprovalType(String approvalType) {
        this.approvalType = approvalType;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }

    public static class DecisionBuilder {
        private String correlationId;
        private boolean actionRequired;
        private String strategy;
        private String skill;
        private String reasoning;
        private double confidenceScore;
        private boolean policyApproved;
        private String approvalType;
        private Instant decidedAt;

        public DecisionBuilder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public DecisionBuilder actionRequired(boolean actionRequired) {
            this.actionRequired = actionRequired;
            return this;
        }

        public DecisionBuilder strategy(String strategy) {
            this.strategy = strategy;
            return this;
        }

        public DecisionBuilder skill(String skill) {
            this.skill = skill;
            return this;
        }

        public DecisionBuilder reasoning(String reasoning) {
            this.reasoning = reasoning;
            return this;
        }

        public DecisionBuilder confidenceScore(double confidenceScore) {
            this.confidenceScore = confidenceScore;
            return this;
        }

        public DecisionBuilder policyApproved(boolean policyApproved) {
            this.policyApproved = policyApproved;
            return this;
        }

        public DecisionBuilder approvalType(String approvalType) {
            this.approvalType = approvalType;
            return this;
        }

        public DecisionBuilder decidedAt(Instant decidedAt) {
            this.decidedAt = decidedAt;
            return this;
        }

        public Decision build() {
            return new Decision(this);
        }
    }
}
