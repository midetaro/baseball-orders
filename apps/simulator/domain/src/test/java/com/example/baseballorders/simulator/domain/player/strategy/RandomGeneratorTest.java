package com.example.baseballorders.simulator.domain.player.strategy;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RandomGeneratorTest {

    @Test
    @DisplayName("確率判定の乱数はfloatへの丸めを含めて0以上1以下の有限値を返す")
    void generatesFiniteProbabilityDraw() {
        // given
        // 実物の乱数生成を使い、乱数の値や頻度ではなく保証された範囲を検証する。

        // when
        float result = RandomGenerator.nextFloat();

        // then
        assertAll(
                () -> assertTrue(Float.isFinite(result)),
                () -> assertTrue(result >= 0.0f),
                () -> assertTrue(result <= 1.0f));
    }
}
