package com.aces.ingestion.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ValidationService {

    public void validate(String payload) {
        if (!StringUtils.hasText(payload)) {
            throw new IllegalArgumentException("Signal payload must not be empty");
        }
    }
}
