package com.example.baseballorders.simulator.domain.game.inning;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.baseballorders.simulator.domain.game.GameBattingContext;
import com.example.baseballorders.simulator.domain.player.BatterTestData;
import com.example.baseballorders.simulator.domain.player.strategy.ScriptedRandom;
import com.example.baseballorders.simulator.domain.statistics.GameStatistics;
import com.example.baseballorders.simulator.domain.statistics.GameStatisticsBuilder;
import com.example.baseballorders.simulator.domain.statistics.StatisticsAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 打順の進行を観測するシナリオ。
 *
 * <p>実物: GameBattingContext, InningStateContext, 8種の BasesState, BatterEntity, 各 Strategy,
 * GameStatisticsRecorder
 *
 * <p>モック: RandomGenerator（乱数列を固定）
 *
 * <p>担保する疎通: 打席の完了 -> 打順の前進 -> 9 人を過ぎたあとの打順の巻き戻し先
 *
 * <p>担保しないもの: 打席が完了しない場合の打順（GameStateLifecycleTest が盗塁でのイニング終了で担保する）
 *
 * <h2>これは現状の挙動を固定するテストである</h2>
 *
 * <p>{@code GameBattingContext#nextAtBat} は打順を次のように進める。
 *
 * <pre>
 * if (numberOfNextBatter == 8) { numberOfNextBatter = 0; }
 * numberOfNextBatter++;
 * </pre>
 *
 * <p>index 8（9 番打者）の打席後は 0 へ戻してから {@code ++} されるため、次は index 0（1 番打者）ではなく index 1（2 番打者）になる。 結果として 1
 * 番打者は試合の最初の 1 打席しか回ってこない。
 *
 * <p>このテストは意図した挙動ではなく、現在の挙動を固定する。打順は公開挙動なので、修正には 独立した feature specification が必要である（{@code
 * apps/simulator/docs/test-strategy.md} の §11-6）。 修正するときは、期待値を「10 打席目は 1
 * 番打者」へ書き換えることがそのまま仕様変更の宣言になる。
 */
class BattingOrderInningScenarioTest {

    private static final int AT_BATS = 10;

    private static GameStatistics expectedStatistics() {
        return GameStatisticsBuilder.gameStatistics()
                .hitCount(1)
                .singleHitCount(0)
                .doubleHitCount(0)
                .tripleHitCount(0)
                .homeRunCount(1)
                .soloHomeRunCount(1)
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
    @DisplayName("9番打者の次は1番打者ではなく2番打者に戻る（現状の挙動）")
    void returnsToSecondBatterAfterNinthBatter() {
        // given
        // 1番打者だけがこの乱数で本塁打になり、2番以降は三振になる打順。
        // 10打席目が1番打者なら本塁打が2本・2得点、2番打者なら本塁打1本・1得点になる。
        var sut = new GameBattingContext(BatterTestData.distinctLineUp());

        // when
        try (ScriptedRandom random =
                ScriptedRandom.repeating(BatterTestData.DISTINCT_LINE_UP_DRAW, AT_BATS)) {
            for (int atBat = 0; atBat < AT_BATS; atBat++) {
                sut.nextAtBat();
            }

            // then
            GameStatistics statistics = sut.getGameStatistics();
            assertAll(
                    () -> assertEquals(1, sut.getTotalScore(), "1番打者は最初の1打席しか回らないため1得点であること"),
                    () -> assertEquals(1, statistics.homeRunCount(), "本塁打は1本だけであること"),
                    () -> assertEquals(4, sut.getInning(), "8三振で3イニングが終わり4回に入ること"),
                    () -> assertEquals(expectedStatistics(), statistics),
                    () -> StatisticsAssertions.assertConsistent(statistics),
                    () -> assertEquals(AT_BATS, random.consumedCount(), "1打席あたり乱数は打撃の1個だけであること"),
                    () -> random.assertFullyConsumed());
        }
    }
}
