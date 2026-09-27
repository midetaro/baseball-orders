package com.example.baseballorders.simulator.domain.game.inning;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.baseballorders.simulator.domain.game.GameStateTestFixture;
import com.example.baseballorders.simulator.domain.player.BatterTestData;
import com.example.baseballorders.simulator.domain.player.strategy.Draws;
import com.example.baseballorders.simulator.domain.player.strategy.ScriptedRandom;
import com.example.baseballorders.simulator.domain.statistics.GameStatistics;
import com.example.baseballorders.simulator.domain.statistics.GameStatisticsBuilder;
import com.example.baseballorders.simulator.domain.statistics.StatisticsAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 三者凡退のイニングシナリオ。
 *
 * <p>実物: GameBattingContext, InningStateContext, 8種の BasesState, BatterEntity, 各 Strategy,
 * GameStatisticsRecorder
 *
 * <p>モック: RandomGenerator（乱数列を固定）
 *
 * <p>担保する疎通: 打席 -> 打撃フェーズ -> アウト加算 -> 三死でのイニング完了 -> 試合回数の前進
 *
 * <p>担保しないもの: 走者がいる場合の盗塁・バントフェーズ（他のシナリオが担保する）
 *
 * <p>乱数 3 個だけでイニングが閉じる最小経路であり、盗塁・バントフェーズが 走者なしでは乱数を消費しないことを消費個数で固定する。
 */
class ThreeUpThreeDownInningScenarioTest {

    private static GameStatistics expectedStatistics() {
        return GameStatisticsBuilder.gameStatistics()
                .hitCount(0)
                .singleHitCount(0)
                .doubleHitCount(0)
                .tripleHitCount(0)
                .homeRunCount(0)
                .soloHomeRunCount(0)
                .twoRunHomeRunCount(0)
                .threeRunHomeRunCount(0)
                .grandSlamCount(0)
                .buntCount(0)
                .stealCount(0)
                .buntFailureCount(0)
                .stealFailureCount(0)
                .advancingBuntCount(0)
                .squeezeBuntCount(0)
                .advancingBuntFailureCount(0)
                .squeezeBuntFailureCount(0)
                .stealToSecondCount(0)
                .stealToThirdCount(0)
                .build();
    }

    @Test
    @DisplayName("三振3つで無得点のままイニングが完了し、乱数を3個だけ消費する")
    void completesInningWithThreeStrikeouts() {
        // given
        var sut = GameStateTestFixture.game(BatterTestData.uniformLineUp());

        // when
        try (ScriptedRandom random =
                ScriptedRandom.of(
                        Draws.STRIKEOUT, // 1番: 走者なし -> 三振(1死)
                        Draws.STRIKEOUT, // 2番: 走者なし -> 三振(2死)
                        Draws.STRIKEOUT // 3番: 走者なし -> 三振(3死、イニング完了)
                        )) {
            while (sut.getInning() == 1) {
                sut.nextAtBat();
            }

            // then
            GameStatistics statistics = sut.getGameStatistics();
            assertAll(
                    () -> assertEquals(0, sut.getTotalScore(), "無得点であること"),
                    () -> assertEquals(2, sut.getInning(), "2回へ進むこと"),
                    () -> assertEquals(expectedStatistics(), statistics, "統計は全項目0であること"),
                    () -> StatisticsAssertions.assertConsistent(statistics),
                    () -> assertEquals(3, random.consumedCount(), "乱数は打撃の3個だけであること"),
                    () -> random.assertFullyConsumed());
        }
    }
}
