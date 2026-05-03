package com.aces.ingestion.client;

import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class EvaluationClient {

    private final RestTemplate restTemplate;

    public EvaluationClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ResponseEntity<String> sendToEvaluation(String payload) {
        return restTemplate.postForEntity("http://localhost:8081/api/v1/evaluate", new HttpEntity<>(payload), String.class);
    }
}
