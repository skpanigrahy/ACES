package com.aces.ingestion.controller;

import com.aces.ingestion.service.IngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/signals")
public class SignalController {

    private final IngestionService ingestionService;

    public SignalController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping
    public ResponseEntity<String> receiveSignal(@RequestBody String payload) {
        ingestionService.forwardSignal(payload);
        return ResponseEntity.accepted().body("signal received");
    }
}
