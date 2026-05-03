package com.aces.ingestion.service;

import com.aces.common.model.Signal;
import com.aces.ingestion.client.EvaluationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.UUID;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final ValidationService validationService;
    private final EvaluationClient evaluationClient;
    private final RestTemplate restTemplate;

    public IngestionService(ValidationService validationService, EvaluationClient evaluationClient, RestTemplate restTemplate) {
        this.validationService = validationService;
        this.evaluationClient = evaluationClient;
        this.restTemplate = restTemplate;
    }

    public void forwardSignal(String payload) {
        validationService.validate(payload);
        evaluationClient.sendToEvaluation(payload);
    }

    public String processSignal(Signal signal) {
        // 1. Generate ID
        String correlationId = "ACES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        // Assuming Signal has a setTimestamp method, but since it's not, I'll comment it out or adjust
        // signal.setTimestamp(Instant.now());

        // 2. Call Evaluation Service
        try {
            log.info("[INGESTION] Forwarding to Evaluation Service...");

            // URL matches application.yml of Evaluation Service (port 8081)
            String url = "http://localhost:8081/api/v1/evaluate?correlationId=" + correlationId;

            // Send signal and get result
            restTemplate.postForObject(url, signal, String.class);

            log.info("[INGESTION] Signal forwarded successfully.");
        } catch (Exception e) {
            log.error("[INGESTION] Failed to forward signal to evaluation.", e);
        }

        return correlationId;
    }
}
