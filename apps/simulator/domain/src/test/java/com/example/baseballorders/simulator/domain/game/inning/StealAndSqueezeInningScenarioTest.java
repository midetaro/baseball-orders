package com.example.baseballorders.simulator.domain.game.inning;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.baseballorders.simulator.domain.game.GameStateTestFixture;
import com.example.baseballorders.simulator.domain.player.BatterTestData;
import com.example.baseballorders.simulator.domain.player.strategy.Draws;
import com.example.baseballorders.simulator.domain.player.strategy.ScriptedRandom;
import com.example.baseballorders.simulator.domain.rule.SimulationRulesTestData;
import com.example.baseballorders.simulator.domain.statistics.GameStatistics;
import com.example.baseballorders.simulator.domain.statistics.GameStatisticsBuilder;
import com.example.baseballorders.simulator.domain.statistics.StatisticsAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 盗塁とバントの成功経路をつなげたイニングシナリオ。
 *
 * <p>実物: GameBattingContext, InningStateContext, 8種の BasesState, BatterEntity, 各 Strategy,
 * GameStatisticsRecorder
 *
 * <p>モック: RandomGenerator（乱数列を固定）
 *
 * <p>担保する疎通: 打席 -> 盗塁フェーズ -> バントフェーズ -> 打撃フェーズ -> 得点・アウト -> イニング完了、および 進塁バントとスクイズが別項目として記録されること
 *
 * <p>担保しないもの: 盗塁・バントの失敗経路（他のシナリオが担保する）
 *
 * <p>打順は積極バント戦略にする。標準バント戦略は無死でしかバントを試みないため、 一死からのスクイズを脚本に載せられない。
 */
class StealAndSqueezeInningScenarioTest {

    private static GameStatistics expectedStatistics() {
        return GameStatisticsBuilder.gameStatistics()
                .hitCount(1)
                .singleHitCount(1)
                .doubleHitCount(0)
                .tripleHitCount(0)
                .homeRunCount(0)
                .soloHomeRunCount(0)
                .twoRunHomeRunCount(0)
                .threeRunHomeRunCount(0)
                .grandSlamCount(0)
                .buntCount(2)
                .stealCount(1)
                .buntFailureCount(0)
                .stealFailureCount(0)
                .advancingBuntCount(1)
                .squeezeBuntCount(1)
                .advancingBuntFailureCount(0)
                .squeezeBuntFailureCount(0)
                .stealToSecondCount(1)
                .stealToThirdCount(0)
                .build();
    }

    @Test
    @DisplayName("単打から二盗成功・進塁バント成功・スクイズ成功で1点を取りイニングが完了する")
    void scoresOneRunByStealAndSqueeze() {
        // given
        var sut =
                GameStateTestFixture.game(
                        BatterTestData.lineUpOf(
                                BatterTestData.batter(
                                        SimulationRulesTestData.strategies()
                                                .middleDistanceHittingStrategy(),
                                        SimulationRulesTestData.strategies().standardSteal(),
                                        SimulationRulesTestData.strategies().eagerBunt())));

        // when
        try (ScriptedRandom random =
                ScriptedRandom.of(
                        Draws.SINGLE, // 1番: 走者なし -> 単打(一塁)
                        Draws.STEAL_TO_SECOND_SUCCESS, // 2番打席前: 一塁走者が二盗成功(二塁)
                        Draws.BUNT_SUCCESS, // 2番: 二塁 -> 進塁バント成功(1死、三塁)
                        Draws.BUNT_SUCCESS, // 3番: 一死三塁 -> スクイズ成功(2死、1得点)
                        Draws.STRIKEOUT // 4番: 走者なし -> 三振(3死、イニング完了)
                        )) {
            while (sut.getInning() == 1) {
                sut.nextAtBat();
            }

            // then
            GameStatistics statistics = sut.getGameStatistics();
            assertAll(
                    () -> assertEquals(1, sut.getTotalScore(), "スクイズで1点入ること"),
                    () -> assertEquals(2, sut.getInning(), "2回へ進むこと"),
                    () -> assertEquals(expectedStatistics(), statistics),
                    () -> StatisticsAssertions.assertConsistent(statistics),
                    () -> assertEquals(5, random.consumedCount(), "乱数は打撃2個・盗塁1個・バント2個であること"),
                    () -> random.assertFullyConsumed());
        }
    }
}
