package com.aces.evaluation.drift;

import com.aces.common.model.Signal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CertDriftDetector {

    private static final Logger log = LoggerFactory.getLogger(CertDriftDetector.class);

    public boolean detectDrift(Signal signal) {
        String expected = signal.getPayload() != null ? signal.getPayload().get("expected_thumbprint") : null;
        String observed = signal.getPayload() != null ? signal.getPayload().get("observed_thumbprint") : null;

        if (expected == null || observed == null) {
            log.warn("[CERT_DETECTOR] Payload missing thumbprints");
            return false;
        }

        boolean mismatch = !expected.equals(observed);

        if (mismatch) {
            log.info("[CERT_DETECTOR] Mismatch detected! Expected={}, Observed={}", expected, observed);
        }

        return mismatch;
    }
}