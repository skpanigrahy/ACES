package com.aces.common.util;

import java.util.UUID;

public final class CorrelationIDGenerator {

    private CorrelationIDGenerator() {
    }

    public static String nextId() {
        return UUID.randomUUID().toString();
    }
}
