package com.aces.decision.engine;

import com.aces.common.model.Decision;
import com.aces.common.model.EvaluationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class DecisionEngine {

    private static final Logger log = LoggerFactory.getLogger(DecisionEngine.class);

    public Decision evaluate(EvaluationResult evaluation) {
        Decision.DecisionBuilder builder = Decision.builder()
                .correlationId(evaluation.getCorrelationId())
                .decidedAt(Instant.now())
                .confidenceScore(0.95); // MVP baseline

        // 1. Check if action is needed
        if (!evaluation.isDriftDetected()) {
            return builder
                    .actionRequired(false)
                    .reasoning("No drift detected. No action required.")
                    .build();
        }

        // 2. Select strategy & skill based on drift type
        String strategy = selectStrategy(evaluation);
        String skill = selectSkill(evaluation.getDriftType());
        String reasoning = buildReasoning(evaluation, strategy);

        // 3. Mock Policy Check (MVP: always approve safe actions)
        boolean policyApproved = true; 

        return builder
                .actionRequired(true)
                .strategy(strategy)
                .skill(skill)
                .reasoning(reasoning)
                .policyApproved(policyApproved)
                .approvalType("AUTO")
                .build();
    }

    private String selectStrategy(EvaluationResult eval) {
        return switch (eval.getDriftType().toUpperCase()) {
            case "CERT_MISMATCH" -> "RESTART";      // Fast, low-risk
            case "THRESHOLD_BREACH" -> "RELOAD";    // Config reload
            case "POLICY_VIOLATION" -> "DEPLOY";    // Full pipeline
            default -> "RESTART";
        };
    }

    private String selectSkill(String driftType) {
        return switch (driftType.toUpperCase()) {
            case "CERT_MISMATCH" -> "fix-cert-runtime-drift";
            case "THRESHOLD_BREACH" -> "handle-latency-incident";
            case "POLICY_VIOLATION" -> "enforce-crypto-policy";
            default -> "generic-remediation";
        };
    }

    private String buildReasoning(EvaluationResult eval, String strategy) {
        return String.format("%s detected. Strategy: %s (confidence: 95%%). Selected for fastest safe resolution.", 
                             eval.getDriftType(), strategy);
    }
}
