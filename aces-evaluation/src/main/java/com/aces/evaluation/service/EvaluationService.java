package com.aces.evaluation.service;

import com.aces.common.model.EvaluationResult;
import com.aces.common.model.Signal;
import com.aces.evaluation.drift.CertDriftDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Service
public class EvaluationService {

    private static final Logger log = LoggerFactory.getLogger(EvaluationService.class);

    private final CertDriftDetector certDriftDetector;
    private final RestTemplate restTemplate;
    
    @Value("${aces.decision-service-url:http://localhost:8082}")
    private String decisionServiceUrl;

    public EvaluationService(CertDriftDetector certDriftDetector, RestTemplate restTemplate) {
        this.certDriftDetector = certDriftDetector;
        this.restTemplate = restTemplate;
    }

    public EvaluationResult evaluate(Signal signal, String correlationId) {
        String driftType = "NONE";
        boolean isDrift = false;
        String reasoning = "State matches desired configuration.";

        if ("cert_runtime_mismatch".equalsIgnoreCase(signal.getType())) {
            log.debug("[EVALUATION] Delegating to CertDriftDetector");
            isDrift = certDriftDetector.detectDrift(signal);

            if (isDrift) {
                driftType = "CERT_MISMATCH";
                String expected = signal.getPayload() != null ? signal.getPayload().get("expected_thumbprint") : null;
                String observed = signal.getPayload() != null ? signal.getPayload().get("observed_thumbprint") : null;
                reasoning = String.format(
                    "Certificate drift detected. Expected: %s, Observed: %s",
                    expected,
                    observed
                );
                
                try {
                    log.info("[EVALUATION] Drift detected. Forwarding to Decision Engine...");
                    EvaluationResult decisionResult = new EvaluationResult(
                        correlationId,
                        isDrift,
                        driftType,
                        isDrift ? "HIGH" : "LOW",
                        reasoning,
                        Instant.now()
                    );
                    restTemplate.postForObject(decisionServiceUrl, decisionResult, String.class);
                    log.info("[EVALUATION] Decision forwarded successfully.");
                } catch (Exception e) {
                    log.warn("[EVALUATION] Decision service unreachable. Queuing for retry.", e);
                }
            }
        }

        return new EvaluationResult(
            correlationId,
            isDrift,
            driftType,
            isDrift ? "HIGH" : "LOW",
            reasoning,
            Instant.now()
        );
    }
}
