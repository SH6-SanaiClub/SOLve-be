package com.shinhan.esg_be.domain.bank.service.screening;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class MockVirtualCbClient {

    public VirtualCbAssessmentResult assess(Long userId) {
        return new VirtualCbAssessmentResult(
                generateCbScore(),
                generateTelecomDelinquency()
        );
    }

    private int generateCbScore() {
        int roll = ThreadLocalRandom.current().nextInt(100);

        if (roll < 95) {
            return ThreadLocalRandom.current().nextInt(600, 1001);
        }

        if (roll < 98) {
            return ThreadLocalRandom.current().nextInt(500, 600);
        }

        return ThreadLocalRandom.current().nextInt(300, 500);
    }

    private boolean generateTelecomDelinquency() {
        return ThreadLocalRandom.current().nextInt(100) < 3;
    }
}
