package org.example.domain.game;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.mock.Mock_Batters;
import org.example.domain.model.GameContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @DisplayName("得点が加算されること")
    @Test
    public void addScoreTest() {
        // given when
        GameContext gameContext = new GameContext(Mock_Batters.mock());
        gameContext.addScore(3);
        // then
        assertEquals(3, gameContext.getTotalScore());
    }
}
