package com.example.baseballorders.backend.application;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimulationLimitsTest {

    @Test
    @DisplayName("設定から与えた上限値をそのまま保持する")
    void keepsConfiguredValues() {
        // given
        var resultTimeout = Duration.ofSeconds(45);

        // when
        var sut =
                SimulationLimitsBuilder.simulationLimits()
                        .resultTimeout(resultTimeout)
                        .maximumAverageHitAverage(0.320f)
                        .build();

        // then
        assertAll(
                () -> assertEquals(resultTimeout, sut.resultTimeout()),
                () -> assertEquals(0.320f, sut.maximumAverageHitAverage()));
    }

    @Test
    @DisplayName("結果待機時間が未設定の場合は生成を拒否する")
    void rejectsMissingResultTimeout() {
        // given
        var builder =
                SimulationLimitsBuilder.simulationLimits()
                        .resultTimeout(null)
                        .maximumAverageHitAverage(0.350f);

        // when
        var exception = assertThrows(NullPointerException.class, builder::build);

        // then
        assertAll(() -> assertEquals("resultTimeout must not be null", exception.getMessage()));
    }
}
