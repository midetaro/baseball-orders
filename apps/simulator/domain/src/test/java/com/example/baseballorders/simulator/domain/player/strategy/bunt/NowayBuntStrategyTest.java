package com.example.baseballorders.simulator.domain.player.strategy.bunt;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.baseballorders.simulator.domain.play.BuntResult;
import com.example.baseballorders.simulator.domain.play.BuntType;
import com.example.baseballorders.simulator.domain.play.OutCount;
import com.example.baseballorders.simulator.domain.player.strategy.ScriptedRandom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * バントしない戦略の確率仕様。
 *
 * <p>アウトカウントとバント種別にかかわらず乱数を 1 個も引かない。乱数を消費しないことは シナリオテストの乱数列を組み立てるうえでの前提なので、消費個数まで検証する。
 */
class NowayBuntStrategyTest {

    @DisplayName("バントしない戦略はアウトカウント・バント種別にかかわらず乱数を引かず試行しない")
    @ParameterizedTest
    @EnumSource(OutCount.class)
    void neverAttemptsBunt(OutCount outCount) {
        // given
        var sut = new NowayBuntStrategy();

        // when
        BuntResult advancingResult;
        BuntResult squeezeResult;
        try (ScriptedRandom scriptedRandom = ScriptedRandom.of()) {
            advancingResult = sut.bunt(outCount, BuntType.ADVANCING);
            squeezeResult = sut.bunt(outCount, BuntType.SQUEEZE);

            // then
            assertAll(
                    () -> assertEquals(BuntResult.NOT_TRY, advancingResult),
                    () -> assertEquals(BuntResult.NOT_TRY, squeezeResult),
                    () -> assertEquals(0, scriptedRandom.consumedCount()),
                    () -> scriptedRandom.assertFullyConsumed());
        }
    }
}
