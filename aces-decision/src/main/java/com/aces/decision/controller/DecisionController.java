package com.aces.decision.controller;

import com.aces.common.model.EvaluationResult;
import com.aces.common.model.Decision;
import com.aces.decision.engine.DecisionEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/decide")
public class DecisionController {

    private static final Logger log = LoggerFactory.getLogger(DecisionController.class);
    
    private final DecisionEngine decisionEngine;

    public DecisionController(DecisionEngine decisionEngine) {
        this.decisionEngine = decisionEngine;
    }

    @PostMapping
    public Map<String, Object> makeDecision(@RequestBody EvaluationResult evaluation) {
        log.info("[DECISION] Processing evaluation for correlationId={}", evaluation.getCorrelationId());
        
        Decision decision = decisionEngine.evaluate(evaluation);
        
        log.info("[DECISION] Result: Action={}, Strategy={}, Skill={}, Reasoning={}", 
                 decision.isActionRequired(), 
                 decision.getStrategy(), 
                 decision.getSkill(), 
                 decision.getReasoning());
        
        return Map.of(
            "correlationId", decision.getCorrelationId(),
            "decision", decision
        );
    }
}