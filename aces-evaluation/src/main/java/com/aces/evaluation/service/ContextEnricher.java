package com.aces.evaluation.service;

import org.springframework.stereotype.Component;

@Component
public class ContextEnricher {

    public void enrich(String payload) {
        // add metadata or context before evaluation
    }
}
