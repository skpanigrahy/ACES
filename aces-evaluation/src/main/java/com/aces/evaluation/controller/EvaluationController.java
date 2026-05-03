package com.aces.evaluation.controller;

import com.aces.common.model.EvaluationResult;
import com.aces.common.model.Signal;
import com.aces.evaluation.service.EvaluationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/evaluate")
public class EvaluationController {

    private static final Logger log = LoggerFactory.getLogger(EvaluationController.class);

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping
    public Map<String, Object> evaluateState(
            @RequestParam String correlationId,
            @RequestBody Signal signal) {

        log.info("[EVALUATION] Processing signal type={} for correlationId={}", 
                 signal.getType(), correlationId);

        EvaluationResult result = evaluationService.evaluate(signal, correlationId);

        log.info("[EVALUATION] Result: Drift={}, Type={}, Reasoning={}", 
                 result.isDriftDetected(), result.getDriftType(), result.getReasoning());

        return Map.of(
            "correlationId", correlationId,
            "evaluationResult", result
        );
    }
}
