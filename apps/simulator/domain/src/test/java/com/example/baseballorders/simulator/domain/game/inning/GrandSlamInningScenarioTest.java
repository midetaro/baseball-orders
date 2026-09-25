package com.example.baseballorders.simulator.domain.game.inning;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.baseballorders.simulator.domain.game.GameBattingContext;
import com.example.baseballorders.simulator.domain.player.BatterTestData;
import com.example.baseballorders.simulator.domain.player.strategy.Draws;
import com.example.baseballorders.simulator.domain.player.strategy.ScriptedRandom;
import com.example.baseballorders.simulator.domain.statistics.GameStatistics;
import com.example.baseballorders.simulator.domain.statistics.GameStatisticsBuilder;
import com.example.baseballorders.simulator.domain.statistics.StatisticsAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 満塁本塁打のイニングシナリオ。
 *
 * <p>実物: GameBattingContext, InningStateContext, 8種の BasesState, BatterEntity, 各 Strategy,
 * GameStatisticsRecorder
 *
 * <p>モック: RandomGenerator（乱数列を固定）
 *
 * <p>担保する疎通: 単打の積み上げによる満塁 -> 本塁打 -> 打撃前の走者数から満塁本塁打として記録されること -> 4得点 -> イニング完了
 *
 * <p>担保しないもの: 本塁打以外の満塁からの進塁規則（BasesStateTransitionTest が担保する）
 *
 * <p>打順は盗塁もバントもしない戦略にする。基準打者の標準バント戦略は無死で必ずバントを試みるため、 単打を 3 本積み上げて満塁を作れない。
 */
class GrandSlamInningScenarioTest {

    private static GameStatistics expectedStatistics() {
        return GameStatisticsBuilder.gameStatistics()
                .hitCount(4)
                .singleHitCount(3)
                .doubleHitCount(0)
                .tripleHitCount(0)
                .homeRunCount(1)
                .soloHomeRunCount(0)
                .twoRunHomeRunCount(0)
                .threeRunHomeRunCount(0)
                .grandSlamCount(1)
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
    @DisplayName("単打3本で満塁にしてから本塁打を打つと満塁本塁打として4点入る")
    void scoresFourRunsByGrandSlam() {
        // given
        var sut = new GameBattingContext(BatterTestData.swingOnlyLineUp());

        // when
        try (ScriptedRandom random =
                ScriptedRandom.of(
                        Draws.SINGLE, // 1番: 走者なし -> 単打(一塁)
                        Draws.SINGLE, // 2番: 一塁 -> 単打(一二塁)
                        Draws.SINGLE, // 3番: 一二塁 -> 単打(満塁)
                        Draws.HOMER, // 4番: 満塁 -> 本塁打(満塁本塁打、4得点)
                        Draws.STRIKEOUT, // 5番: 走者なし -> 三振(1死)
                        Draws.STRIKEOUT, // 6番: 走者なし -> 三振(2死)
                        Draws.STRIKEOUT // 7番: 走者なし -> 三振(3死、イニング完了)
                        )) {
            while (sut.getInning() == 1) {
                sut.nextAtBat();
            }

            // then
            GameStatistics statistics = sut.getGameStatistics();
            assertAll(
                    () -> assertEquals(4, sut.getTotalScore(), "満塁本塁打で4点入ること"),
                    () -> assertEquals(2, sut.getInning(), "2回へ進むこと"),
                    () -> assertEquals(expectedStatistics(), statistics),
                    () -> StatisticsAssertions.assertConsistent(statistics),
                    () -> assertEquals(7, random.consumedCount(), "盗塁もバントもしない戦略なので乱数は打撃の7個だけであること"),
                    () -> random.assertFullyConsumed());
        }
    }
}
