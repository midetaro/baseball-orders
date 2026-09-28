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
 * <p>進塁先にかかわらず乱数を 1 個も引かない。乱数を消費しないことはシナリオテストの 乱数列を組み立てるうえでの前提なので、消費個数まで検証する。
 */
class NowayStealStrategyTest {

    @Test
    @DisplayName("盗塁しない戦略は乱数を引かず両塁への試行を拒む")
    void neverAttemptsEitherBase() {
        // given
        var sut = new NowayStealStrategy();

        // when
        StealResult toSecond;
        StealResult toThird;
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of()) {
            toSecond = sut.runToDouble();
            toThird = sut.runToTriple();

            // then
            assertAll(
                    () -> assertEquals(StealResult.NOT_TRY, toSecond),
                    () -> assertEquals(StealResult.NOT_TRY, toThird),
                    () -> assertEquals(0, scriptedRandom.consumedCount()),
                    () -> scriptedRandom.assertFullyConsumed());
        }
    }
}
