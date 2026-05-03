package com.aces.ingestion.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "aces-ingestion");
    }

    @GetMapping("/health")
    public Map<String, String> healthEndpoint() {
        return Map.of("status", "UP", "service", "aces-ingestion");
    }
}