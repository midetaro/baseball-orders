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
 * 盗塁死を含むイニングシナリオ。
 *
 * <p>実物: GameBattingContext, InningStateContext, 8種の BasesState, BatterEntity, 各 Strategy,
 * GameStatisticsRecorder
 *
 * <p>モック: RandomGenerator（乱数列を固定）
 *
 * <p>担保する疎通: 打席 -> 盗塁フェーズの失敗 -> アウト加算と走者除去 -> 同じ打席で打撃フェーズへ進むこと ->
 * イニング完了、および盗塁死が成功盗塁数ではなく盗塁失敗数に記録されること
 *
 * <p>担保しないもの: 盗塁死でイニングが終わる場合の打順の扱い（GameStateLifecycleTest が担保する）
 *
 * <p>盗塁死はイニングを終わらせない限り打席を完了させないので、同じ打席で打撃まで進み 乱数を 2 個消費する。この消費順は乱数列の組み立ての前提なので個数まで固定する。
 */
class StealFailureInningScenarioTest {

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
                .buntCount(0)
                .stealCount(0)
                .buntFailureCount(0)
                .stealFailureCount(1)
                .advancingBuntCount(0)
                .squeezeBuntCount(0)
                .advancingBuntFailureCount(0)
                .squeezeBuntFailureCount(0)
                .stealToSecondCount(0)
                .stealToThirdCount(0)
                .build();
    }

    @Test
    @DisplayName("単打から二盗失敗で走者を失い、同じ打席で打撃して3死でイニングが完了する")
    void losesRunnerByStealFailureAndCompletesInning() {
        // given
        var sut = GameStateTestFixture.game(BatterTestData.uniformLineUp());

        // when
        try (ScriptedRandom random =
                ScriptedRandom.of(
                        Draws.SINGLE, // 1番: 走者なし -> 単打(一塁)
                        Draws.STEAL_TO_SECOND_FAILURE, // 2番打席前: 一塁走者が二盗失敗(1死、走者なし)
                        Draws.STRIKEOUT, // 2番: 盗塁死では打席が完了しないためそのまま三振(2死)
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
                    () -> assertEquals(expectedStatistics(), statistics),
                    () -> StatisticsAssertions.assertConsistent(statistics),
                    () -> assertEquals(4, random.consumedCount(), "乱数は打撃3個・盗塁1個であること"),
                    () -> random.assertFullyConsumed());
        }
    }
}
