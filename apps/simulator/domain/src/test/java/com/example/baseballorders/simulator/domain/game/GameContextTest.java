package com.example.baseballorders.simulator.domain.game;

import com.example.baseballorders.simulator.domain.model.GameContext;
import com.example.baseballorders.simulator.domain.model.state.NoBasesState;
import com.example.baseballorders.simulator.domain.model.state.SingleBasesState;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@Slf4j
@Getter
public class GameContextTest {

    @DisplayName("addOutCounts() - 分岐網羅テスト")
    @ParameterizedTest(name = "{0}")
    @MethodSource("addOutCountsTestCases")
    public void addOutCountsTest(String description, int initialInning, int addOutCount,
                                 int expectedOutCounts, int expectedInning, boolean expectedGameOver) {
        // given
        GameContext gameContext = new GameContext(Mock_Batters.mock());
        if (initialInning > 1) {
            // 設定するためにリフレクションまたは複数回の呼び出しで調整
            for (int i = 1; i < initialInning; i++) {
                gameContext.addOutCounts(3);
            }
        }
        
        // when
        gameContext.addOutCounts(addOutCount);

        // then
        assertEquals(expectedOutCounts, gameContext.getOutCounts(), description);
        assertEquals(expectedInning, gameContext.getInning(), description);
        assertEquals(expectedGameOver, gameContext.isGameOver(), description);
    }

    static Stream<Arguments> addOutCountsTestCases() {
        return Stream.of(
                // 分岐1: outCounts < 3（goToNextInning呼ばれない）
                arguments(
                        "[分岐1-1] addOutCounts(1) → outCounts=1（goToNextInning スキップ）",
                        1, 1, 1, 1, false
                ),
                arguments(
                        "[分岐1-2] addOutCounts(2) → outCounts=2（goToNextInning スキップ）",
                        1, 2, 2, 1, false
                ),
                // 分岐2a: outCounts >= 3 かつ inning != 9（イニング進行）
                arguments(
                        "[分岐2a] addOutCounts(3)、inning=1 → inning=2, outCounts=0（リセット）",
                        1, 3, 0, 2, false
                ),
                arguments(
                        "[分岐2a] addOutCounts(3)、inning=2 → inning=3, outCounts=0（リセット）",
                        2, 3, 0, 3, false
                ),
                // 分岐2b: outCounts >= 3 かつ inning == 9（ゲーム終了）
                arguments(
                        "[分岐2b] addOutCounts(3)、inning=9 → isGameOver=true, outCounts=0",
                        9, 3, 0, 9, true
                )
        );
    }

    @DisplayName("addScore() - 分岐網羅テスト")
    @ParameterizedTest(name = "{0}")
    @MethodSource("addScoreTestCases")
    public void addScoreTest(String description, long initialScore, long addScore, long expectedScore) {
        // given
        GameContext gameContext = new GameContext(Mock_Batters.mock());
        // 初期スコア設定
        for (int i = 0; i < initialScore; i++) {
            gameContext.addScore(1);
        }

        // when
        gameContext.addScore(addScore);

        // then
        assertEquals(expectedScore, gameContext.getTotalScore(), description);
    }

    static Stream<Arguments> addScoreTestCases() {
        return Stream.of(
                // スコアが正常に加算されるケース
                arguments(
                        "[基本] addScore(0) → totalScore=0",
                        0, 0, 0
                ),
                arguments(
                        "[基本] addScore(1) → totalScore=1",
                        0, 1, 1
                ),
                arguments(
                        "[基本] addScore(3) → totalScore=3",
                        0, 3, 3
                ),
                arguments(
                        "[累積] initialScore=5 + addScore(2) → totalScore=7",
                        5, 2, 7
                )
        );
    }

    @DisplayName("updateBaseState() - 分岐網羅テスト")
    @ParameterizedTest(name = "{0}")
    @MethodSource("updateBaseStateTestCases")
    public void updateBaseStateTest(String description, com.example.baseballorders.simulator.domain.model.state.BasesState newState) {
        // given
        GameContext gameContext = new GameContext(Mock_Batters.mock());

        // when
        gameContext.updateBaseState(newState);

        // then
        assertEquals(newState, gameContext.getCurrentBaseState(), description);
    }

    static Stream<Arguments> updateBaseStateTestCases() {
        return Stream.of(
                arguments(
                        "[初期状態→NoBasesState] 塁が空になる",
                        new NoBasesState()
                ),
                arguments(
                        "[初期状態→SingleBasesState] 1塁に走者がいる状態",
                        new SingleBasesState()
                )
        );
    }

    @DisplayName("setRunnerTo() - 分岐網羅テスト")
    @ParameterizedTest(name = "{0}")
    @MethodSource("setRunnerToTestCases")
    public void setRunnerToTest(String description, int baseNumber, boolean hasBatter, int expectedBaseCount) {
        // given
        GameContext gameContext = new GameContext(Mock_Batters.mock());
        Optional<com.example.baseballorders.simulator.domain.model.player.Batter> batter = hasBatter ? Optional.of(Mock_Batters.mock().get(0)) : Optional.empty();

        // when
        gameContext.setRunnerTo(baseNumber, batter);

        // then
        Optional<com.example.baseballorders.simulator.domain.model.player.Batter> runner = switch (baseNumber) {
            case 1 -> gameContext.getRunnerOnFirstBase();
            case 2 -> gameContext.getRunnerOnSecondBase();
            case 3 -> gameContext.getRunnerOnThirdBase();
            default -> Optional.empty();
        };
        assertEquals(hasBatter, runner.isPresent(), description);
    }

    static Stream<Arguments> setRunnerToTestCases() {
        return Stream.of(
                arguments(
                        "[1塁] 走者を配置",
                        1, true, 1
                ),
                arguments(
                        "[1塁] 走者を削除",
                        1, false, 0
                ),
                arguments(
                        "[2塁] 走者を配置",
                        2, true, 2
                ),
                arguments(
                        "[2塁] 走者を削除",
                        2, false, 0
                ),
                arguments(
                        "[3塁] 走者を配置",
                        3, true, 3
                ),
                arguments(
                        "[3塁] 走者を削除",
                        3, false, 0
                )
        );
    }

    @DisplayName("moveRunnerNthBase() - 分岐網羅テスト")
    @ParameterizedTest(name = "{0}")
    @MethodSource("moveRunnerNthBaseTestCases")
    public void moveRunnerNthBaseTest(String description, int targetBase, boolean expectedFirstEmpty,
                                      boolean expectedSecondEmpty, boolean expectedThirdEmpty) {
        // given
        GameContext gameContext = new GameContext(Mock_Batters.mock());
        var batter = Mock_Batters.mock().get(0);
        gameContext.setRunnerTo(1, Optional.of(batter));
        gameContext.setRunnerTo(2, Optional.of(batter));
        gameContext.setRunnerTo(3, Optional.of(batter));

        // when
        gameContext.moveRunnerNthBase(targetBase);

        // then
        assertEquals(expectedFirstEmpty, gameContext.getRunnerOnFirstBase().isEmpty(), description);
        assertEquals(expectedSecondEmpty, gameContext.getRunnerOnSecondBase().isEmpty(), description);
        assertEquals(expectedThirdEmpty, gameContext.getRunnerOnThirdBase().isEmpty(), description);
    }

    static Stream<Arguments> moveRunnerNthBaseTestCases() {
        return Stream.of(
                // targetBase=1: 1塁へ（1-2-3塁→空-1-2）
                arguments(
                        "[targetBase=1] 1塁へ移動: 全塁走者→1,2塁に配置",
                        1, true, false, false
                ),
                // targetBase=2: 2塁へ（1-2-3塁→空-空-2）
                arguments(
                        "[targetBase=2] 2塁へ移動: 全塁走者→2塁のみ",
                        2, true, true, false
                ),
                // targetBase=3: 3塁へ（1-2-3塁→空-空-空）
                arguments(
                        "[targetBase=3] 3塁へ移動: 全塁走者→全て得点",
                        3, true, true, true
                ),
                // targetBase=4: ホーム（1-2-3塁→空-空-空）
                arguments(
                        "[targetBase=4] ホームへ移動: 全塁走者→全て得点",
                        4, true, true, true
                )
        );
    }

    @DisplayName("isGameOver() - 分岐網羅テスト")
    @ParameterizedTest(name = "{0}")
    @MethodSource("isGameOverTestCases")
    public void isGameOverTest(String description, int addOutCountTimes, boolean expectedGameOver) {
        // given
        GameContext gameContext = new GameContext(Mock_Batters.mock());

        // when - 9回のアウトカウント満了でゲーム終了
        for (int i = 1; i <= addOutCountTimes; i++) {
            gameContext.addOutCounts(3);
        }

        // then
        assertEquals(expectedGameOver, gameContext.isGameOver(), description);
    }

    static Stream<Arguments> isGameOverTestCases() {
        return Stream.of(
                arguments(
                        "[ゲーム中] 1回アウト満了 → isGameOver=false",
                        1, false
                ),
                arguments(
                        "[ゲーム中] 8回アウト満了 → isGameOver=false",
                        8, false
                ),
                arguments(
                        "[ゲーム終了] 9回アウト満了 → isGameOver=true",
                        9, true
                )
        );
    }

    @DisplayName("updateBaseStateOf() - 分岐網羅テスト")
    @ParameterizedTest(name = "{0}")
    @MethodSource("updateBaseStateOfTestCases")
    public void updateBaseStateOfTest(String description, boolean hasFirst, boolean hasSecond, boolean hasThird,
                                      Class<?> expectedStateClass) {
        // given
        GameContext gameContext = new GameContext(Mock_Batters.mock());
        var batter = Mock_Batters.mock().get(0);

        // 各塁に走者を設定
        if (hasFirst) gameContext.setRunnerTo(1, Optional.of(batter));
        if (hasSecond) gameContext.setRunnerTo(2, Optional.of(batter));
        if (hasThird) gameContext.setRunnerTo(3, Optional.of(batter));

        // when
        gameContext.updateBaseStateOf();

        // then
        assertTrue(expectedStateClass.isInstance(gameContext.getCurrentBaseState()), description);
    }

    static Stream<Arguments> updateBaseStateOfTestCases() {
        return Stream.of(
                arguments(
                        "[分岐1] 満塁 → FullBasesState",
                        true, true, true,
                        com.example.baseballorders.simulator.domain.model.state.FullBasesState.class
                ),
                arguments(
                        "[分岐2] 1,2塁 → FirstDoubleBaseState",
                        true, true, false,
                        com.example.baseballorders.simulator.domain.model.state.FirstDoubleBaseState.class
                ),
                arguments(
                        "[分岐3] 1,3塁 → FirstThirdBaseState",
                        true, false, true,
                        com.example.baseballorders.simulator.domain.model.state.FirstThirdBaseState.class
                ),
                arguments(
                        "[分岐4] 1塁のみ → SingleBasesState",
                        true, false, false,
                        SingleBasesState.class
                ),
                arguments(
                        "[分岐5] 2,3塁 → DoubleThirdBaseState",
                        false, true, true,
                        com.example.baseballorders.simulator.domain.model.state.DoubleThirdBaseState.class
                ),
                arguments(
                        "[分岐6] 2塁のみ → DoubleBaseState",
                        false, true, false,
                        com.example.baseballorders.simulator.domain.model.state.DoubleBaseState.class
                ),
                arguments(
                        "[分岐7] 3塁のみ → ThirdBaseState",
                        false, false, true,
                        com.example.baseballorders.simulator.domain.model.state.ThirdBaseState.class
                ),
                arguments(
                        "[分岐8] 塁なし → NoBasesState",
                        false, false, false,
                        NoBasesState.class
                )
        );
    }

    @DisplayName("nextAtBat() - 打者交代テスト")
    @ParameterizedTest(name = "{0}")
    @MethodSource("nextAtBatTestCases")
    public void nextAtBatTest(String description, int numberOfNextBatterBefore, int expectedNumberOfNextBatterAfter) {
        // given
        GameContext gameContext = new GameContext(Mock_Batters.mock());
        // numberOfNextBatter を設定するためにループで実行
        for (int i = 0; i < numberOfNextBatterBefore; i++) {
            gameContext.nextAtBat();
        }

        // when
        gameContext.nextAtBat();

        // then
        assertEquals(expectedNumberOfNextBatterAfter, gameContext.getNumberOfNextBatter(), description);
    }

    static Stream<Arguments> nextAtBatTestCases() {
        return Stream.of(
                // 通常の打者交代（0～7番目）
                arguments(
                        "[打者交代] 0番目 → 1番目",
                        0, 1
                ),
                arguments(
                        "[打者交代] 1番目 → 2番目",
                        1, 2
                ),
                arguments(
                        "[打者交代] 2番目 → 3番目",
                        2, 3
                ),
                arguments(
                        "[打者交代] 3番目 → 4番目",
                        3, 4
                ),
                arguments(
                        "[打者交代] 4番目 → 5番目",
                        4, 5
                ),
                arguments(
                        "[打者交代] 5番目 → 6番目",
                        5, 6
                ),
                arguments(
                        "[打者交代] 6番目 → 7番目",
                        6, 7
                ),
                arguments(
                        "[打者交代] 7番目 → 8番目",
                        7, 8
                ),
                // 順番が一周して最初に戻る（8番目 → 0 → 1）
                arguments(
                        "[打者交代・周回] 8番目 → 1番目",
                        8, 1
                )
        );
    }
}
