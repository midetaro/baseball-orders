package com.example.baseballorders.backend.application;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimulationLimitsTest {

    @Test
    @DisplayName("設定から与えた上限値をそのまま保持する")
    void keepsConfiguredValues() {
        // given
        var maximumAverageHitAverage = 0.320f;

        // when
        var sut =
                SimulationLimitsBuilder.simulationLimits()
                        .maximumAverageHitAverage(maximumAverageHitAverage)
                        .build();

        // then
        assertAll(() -> assertEquals(0.320f, sut.maximumAverageHitAverage()));
    }
}
