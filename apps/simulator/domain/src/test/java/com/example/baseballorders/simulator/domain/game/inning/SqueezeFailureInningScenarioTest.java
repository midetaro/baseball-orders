package com.example.baseballorders.simulator.domain.game.inning;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.baseballorders.simulator.domain.game.GameBattingContext;
import com.example.baseballorders.simulator.domain.player.BatterTestData;
import com.example.baseballorders.simulator.domain.player.strategy.BehaviorStrategies;
import com.example.baseballorders.simulator.domain.player.strategy.Draws;
import com.example.baseballorders.simulator.domain.player.strategy.ScriptedRandom;
import com.example.baseballorders.simulator.domain.statistics.GameStatistics;
import com.example.baseballorders.simulator.domain.statistics.GameStatisticsBuilder;
import com.example.baseballorders.simulator.domain.statistics.StatisticsAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 無死満塁のスクイズ失敗のイニングシナリオ。
 *
 * <p>実物: GameBattingContext, InningStateContext, 8種の BasesState, BatterEntity, 各 Strategy,
 * GameStatisticsRecorder
 *
 * <p>モック: RandomGenerator（乱数列を固定）
 *
 * <p>担保する疎通: 満塁 -> スクイズ失敗 -> 三塁走者と打者の二死適用 -> 残った走者の配置 -> イニング完了、
 * およびスクイズ失敗が進塁バント失敗ではなくスクイズ失敗として記録されること
 *
 * <p>担保しないもの: 二死からのスクイズ失敗（ThirdBaseStateTest などが状態単位で担保する）
 *
 * <p>打順は 4 番だけ標準バント戦略にする。基準打者の標準バント戦略は無死で必ずバントを試みるため、 全員を基準打者にすると単打を 3 本積み上げて満塁を作れない。
 */
class SqueezeFailureInningScenarioTest {

    private static GameStatistics expectedStatistics() {
        return GameStatisticsBuilder.gameStatistics()
                .hitCount(3)
                .singleHitCount(3)
                .doubleHitCount(0)
                .tripleHitCount(0)
                .homeRunCount(0)
                .soloHomeRunCount(0)
                .twoRunHomeRunCount(0)
                .threeRunHomeRunCount(0)
                .grandSlamCount(0)
                .buntCount(0)
                .stealCount(0)
                .buntFailureCount(1)
                .stealFailureCount(0)
                .advancingBuntCount(0)
                .squeezeBuntCount(0)
                .advancingBuntFailureCount(0)
                .squeezeBuntFailureCount(1)
                .stealToSecondCount(0)
                .stealToThirdCount(0)
                .build();
    }

    @Test
    @DisplayName("無死満塁のスクイズ失敗は二死になり三塁走者を失ってイニングが完了する")
    void appliesTwoOutsOnSqueezeFailureAndCompletesInning() {
        // given
        var swingOnly = BatterTestData.swingOnlyBatter();
        var squeezeBatter =
                BatterTestData.batter(
                        BehaviorStrategies.middleDistanceHittingStrategy(),
                        BehaviorStrategies.noSteal(),
                        BehaviorStrategies.standardBunt());
        var sut =
                new GameBattingContext(
                        BatterTestData.lineUpOf(
                                swingOnly,
                                swingOnly,
                                swingOnly,
                                squeezeBatter,
                                swingOnly,
                                swingOnly,
                                swingOnly,
                                swingOnly,
                                swingOnly));

        // when
        try (ScriptedRandom random =
                ScriptedRandom.of(
                        Draws.SINGLE, // 1番: 走者なし -> 単打(一塁)
                        Draws.SINGLE, // 2番: 一塁 -> 単打(一二塁)
                        Draws.SINGLE, // 3番: 一二塁 -> 単打(満塁)
                        Draws.BUNT_FAILURE, // 4番: 無死満塁 -> スクイズ失敗(2死、一二塁)
                        Draws.STRIKEOUT // 5番: 二死一二塁 -> 三振(3死、イニング完了)
                        )) {
            while (sut.getInning() == 1) {
                sut.nextAtBat();
            }

            // then
            GameStatistics statistics = sut.getGameStatistics();
            assertAll(
                    () -> assertEquals(0, sut.getTotalScore(), "スクイズ失敗では得点しないこと"),
                    () -> assertEquals(2, sut.getInning(), "2回へ進むこと"),
                    () -> assertEquals(expectedStatistics(), statistics),
                    () -> StatisticsAssertions.assertConsistent(statistics),
                    () -> assertEquals(5, random.consumedCount(), "乱数は打撃4個・バント1個であること"),
                    () -> random.assertFullyConsumed());
        }
    }
}
