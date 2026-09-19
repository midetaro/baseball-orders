package com.example.baseballorders.simulator.domain.model.statistics;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScoreAccumulatorTest {

    @Test
    @DisplayName("複数の試合終了通知から得点とプレー統計を集計する")
    void aggregatesCompletedGames() {
        // given
        ScoreAccumulator accumulator = new ScoreAccumulator();

        // when
        accumulator.onGameCompleted(8, new GameStatistics(4, 1, 1, 1, 1, 2, 3, 5, 7));
        accumulator.onGameCompleted(2, new GameStatistics(6, 2, 0, 1, 3, 4, 6, 11, 13));

        // then
        ScoreStatistics statistics = accumulator.toScoreStatistics();
        assertAll(
                () -> assertEquals(5.0, statistics.averageScore()),
                () -> assertEquals(5.0, statistics.medianScore()),
                () -> assertEquals(8, statistics.maximumScore()),
                () -> assertEquals(2, statistics.gameCount()),
                () -> assertEquals(Map.of(2, 1, 8, 1), statistics.scoreDistribution()),
                () ->
                        assertEquals(
                                List.of(2, 8),
                                List.copyOf(statistics.scoreDistribution().keySet())),
                () -> assertEquals(10, statistics.homeRunCount()),
                () -> assertEquals(3, statistics.soloHomeRunCount()),
                () -> assertEquals(1, statistics.twoRunHomeRunCount()),
                () -> assertEquals(2, statistics.threeRunHomeRunCount()),
                () -> assertEquals(4, statistics.grandSlamCount()),
                () -> assertEquals(6, statistics.buntCount()),
                () -> assertEquals(9, statistics.stealCount()),
                () -> assertEquals(16, statistics.buntFailureCount()),
                () -> assertEquals(20, statistics.stealFailureCount()));
    }

    @Test
    @DisplayName("段階的ビルダーで得点統計を生成できる")
    void buildsScoreStatisticsWithStagedBuilder() {
        // given
        var builder =
                ScoreStatisticsBuilder.scoreStatistics()
                        .averageScore(3.5)
                        .medianScore(3.0)
                        .maximumScore(9)
                        .gameCount(10)
                        .scoreDistribution(Map.of(3, 4, 5, 6))
                        .homeRunCount(4)
                        .soloHomeRunCount(1)
                        .twoRunHomeRunCount(1)
                        .threeRunHomeRunCount(1)
                        .grandSlamCount(1)
                        .buntCount(2)
                        .stealCount(3)
                        .buntFailureCount(5);

        // when
        var statistics = builder.stealFailureCount(7).build();

        // then
        assertAll(
                () -> assertEquals(3, statistics.stealCount()),
                () -> assertEquals(5, statistics.buntFailureCount()),
                () -> assertEquals(7, statistics.stealFailureCount()));
    }

    @Test
    @DisplayName("終了通知がない場合は統計値を生成できない")
    void rejectsNoCompletedGames() {
        // given
        ScoreAccumulator accumulator = new ScoreAccumulator();

        // when
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, accumulator::toScoreStatistics);

        // then
        assertAll(() -> assertEquals("scores must not be empty", exception.getMessage()));
    }
}
