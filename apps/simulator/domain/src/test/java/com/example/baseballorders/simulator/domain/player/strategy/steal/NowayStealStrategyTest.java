package com.example.baseballorders.simulator.domain.player.strategy.steal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.baseballorders.simulator.domain.play.StealResult;
import com.example.baseballorders.simulator.domain.player.strategy.ScriptedRandom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 盗塁しない戦略の確率仕様。
 *
 * <p>進塁先と成功率にかかわらず乱数を 1 個も引かない。乱数を消費しないことはシナリオテストの 乱数列を組み立てるうえでの前提なので、消費個数まで検証する。
 */
class NowayStealStrategyTest {

    @Test
    @DisplayName("盗塁しない戦略は成功率にかかわらず乱数を引かず両塁への試行を拒む")
    void neverAttemptsEitherBase() {
        // given
        var sut = new NowayStealStrategy();

        // when
        StealResult certainSecond;
        StealResult certainThird;
        StealResult zeroSecond;
        StealResult zeroThird;
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of()) {
            certainSecond = sut.runToDouble(1.0f);
            certainThird = sut.runToTriple(1.0f);
            zeroSecond = sut.runToDouble(0.0f);
            zeroThird = sut.runToTriple(0.0f);

            // then
            assertAll(
                    () -> assertEquals(StealResult.NOT_TRY, certainSecond),
                    () -> assertEquals(StealResult.NOT_TRY, certainThird),
                    () -> assertEquals(StealResult.NOT_TRY, zeroSecond),
                    () -> assertEquals(StealResult.NOT_TRY, zeroThird),
                    () -> assertEquals(0, scriptedRandom.consumedCount()),
                    () -> scriptedRandom.assertFullyConsumed());
        }
    }
}
