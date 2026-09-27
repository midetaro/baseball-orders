package com.example.baseballorders.simulator.domain.game.inning;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.example.baseballorders.simulator.domain.game.GameStateTestFixture;
import com.example.baseballorders.simulator.domain.player.BatterTestData;
import com.example.baseballorders.simulator.domain.player.strategy.Draws;
import com.example.baseballorders.simulator.domain.player.strategy.ScriptedRandom;
import com.example.baseballorders.simulator.domain.statistics.GameStatistics;
import com.example.baseballorders.simulator.domain.statistics.GameStatisticsBuilder;
import com.example.baseballorders.simulator.domain.statistics.StatisticsAssertions;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * 凡退時の先頭走者の進塁判定を両分岐で固定するイニングシナリオ。
 *
 * <p>実物: GameBattingContext, InningStateContext, 8種の BasesState, BatterEntity, 各 Strategy,
 * GameStatisticsRecorder
 *
 * <p>モック: RandomGenerator（乱数列を固定）
 *
 * <p>担保する疎通: 打撃フェーズの凡退 -> 進塁判定のための追加の乱数消費 -> 先頭走者だけの進塁または据え置き -> イニング完了
 *
 * <p>担保しないもの: 一塁・二塁走者の進塁確率（AtBatProcessorTest が塁ごとに担保する）
 *
 * <p>三塁走者を先頭走者にすると進塁がそのまま得点として観測できる。{@code Draws#ADVANCE} と {@code Draws#NO_ADVANCE} の意味を本番経路で固定する
 * のもこのテストの役割である。
 */
class BattedOutAdvanceInningScenarioTest {

    static Stream<Arguments> advanceScenarios() {
        return Stream.of(
                arguments("進塁する乱数では三塁走者が生還して1点入る", Draws.ADVANCE, 1L),
                arguments("進塁しない乱数では三塁走者が残り無得点で終わる", Draws.NO_ADVANCE, 0L));
    }

    private static GameStatistics expectedStatistics() {
        return GameStatisticsBuilder.gameStatistics()
                .hitCount(1)
                .singleHitCount(0)
                .doubleHitCount(0)
                .tripleHitCount(1)
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

    @DisplayName("三塁走者がいる凡退では進塁判定の乱数によって得点が決まる")
    @ParameterizedTest(name = "{0}")
    @MethodSource("advanceScenarios")
    void advancesLeadRunnerOnBattedOut(
            String description, float advancementDraw, long expectedScore) {
        // given
        var sut = GameStateTestFixture.game(BatterTestData.swingOnlyLineUp());

        // when
        try (ScriptedRandom random =
                ScriptedRandom.of(
                        Draws.TRIPLE, // 1番: 走者なし -> 三塁打(三塁)
                        Draws.BATTED_OUT, // 2番: 三塁 -> 凡退
                        advancementDraw, // 2番の凡退に伴う三塁走者の進塁判定(1死)
                        Draws.STRIKEOUT, // 3番: 三振(2死)
                        Draws.STRIKEOUT // 4番: 三振(3死、イニング完了)
                        )) {
            while (sut.getInning() == 1) {
                sut.nextAtBat();
            }

            // then
            GameStatistics statistics = sut.getGameStatistics();
            assertAll(
                    description,
                    () -> assertEquals(expectedScore, sut.getTotalScore()),
                    () -> assertEquals(2, sut.getInning(), "2回へ進むこと"),
                    () -> assertEquals(expectedStatistics(), statistics, "凡退と進塁は統計に記録されないこと"),
                    () -> StatisticsAssertions.assertConsistent(statistics),
                    () -> assertEquals(5, random.consumedCount(), "乱数は打撃4個・進塁判定1個であること"),
                    () -> random.assertFullyConsumed());
        }
    }
}
