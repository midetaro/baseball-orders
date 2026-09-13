package com.example.baseballorders.simulator.domain.model.statistics;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.AbstractList;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScoreStatisticsCalculatorTest {

    @Test
    @DisplayName("試合統計を一度の走査で集計する")
    void aggregatesGameStatisticsInSingleTraversal() {
        // given
        var scoreStatistics = new ScoreStatistics(3.5, 3.0, 9);
        var gameStatistics =
                new SingleTraversalGameStatistics(List.of(new GameStatistics(4, 1, 1, 1, 1, 2, 3)));

        // when
        var statistics = scoreStatistics.withGameStatistics(gameStatistics);

        // then
        assertAll(
                () -> assertEquals(4, statistics.homeRunCount()),
                () -> assertEquals(1, statistics.soloHomeRunCount()),
                () -> assertEquals(1, statistics.twoRunHomeRunCount()),
                () -> assertEquals(1, statistics.threeRunHomeRunCount()),
                () -> assertEquals(1, statistics.grandSlamCount()),
                () -> assertEquals(2, statistics.buntCount()),
                () -> assertEquals(3, statistics.stealCount()));
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
                        .buntCount(2);

        // when
        var statistics = builder.stealCount(3).build();

        // then
        assertAll(() -> assertEquals(3, statistics.stealCount()));
    }

    @Test
    @DisplayName("順不同の奇数件の得点から平均・中央値・最大得点を計算する")
    void calculatesStatisticsForOddNumberOfScores() {
        // given
        var calculator = new ScoreStatisticsCalculator();

        // when
        ScoreStatistics statistics = calculator.calculate(List.of(8, 2, 5));

        // then
        assertAll(
                () -> assertEquals(5.0, statistics.averageScore()),
                () -> assertEquals(5.0, statistics.medianScore()),
                () -> assertEquals(8, statistics.maximumScore()));
    }

    @Test
    @DisplayName("得点ごとの試合数分布と試合数を計算する")
    void calculatesScoreDistributionAndGameCount() {
        // given
        var calculator = new ScoreStatisticsCalculator();

        // when
        ScoreStatistics statistics = calculator.calculate(List.of(8, 2, 5, 2, 8, 8));

        // then
        assertAll(
                () -> assertEquals(6, statistics.gameCount()),
                () -> assertEquals(Map.of(2, 2, 5, 1, 8, 3), statistics.scoreDistribution()),
                () ->
                        assertEquals(
                                List.of(2, 5, 8),
                                List.copyOf(statistics.scoreDistribution().keySet())));
    }

    @Test
    @DisplayName("順不同の偶数件の得点から中央2値の平均を中央値として計算する")
    void calculatesMedianForEvenNumberOfScores() {
        // given
        var calculator = new ScoreStatisticsCalculator();

        // when
        ScoreStatistics statistics = calculator.calculate(List.of(9, 1, 7, 3));

        // then
        assertAll(
                () -> assertEquals(5.0, statistics.averageScore()),
                () -> assertEquals(5.0, statistics.medianScore()),
                () -> assertEquals(9, statistics.maximumScore()));
    }

    @Test
    @DisplayName("得点がない場合は統計値を計算できない")
    void rejectsEmptyScores() {
        // given
        var calculator = new ScoreStatisticsCalculator();

        // when
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> calculator.calculate(List.of()));

        // then
        assertAll(() -> assertEquals("scores must not be empty", exception.getMessage()));
    }

    private static final class SingleTraversalGameStatistics extends AbstractList<GameStatistics> {
        private final List<GameStatistics> statistics;
        private boolean traversed;

        private SingleTraversalGameStatistics(List<GameStatistics> statistics) {
            this.statistics = statistics;
        }

        @Override
        public GameStatistics get(int index) {
            return statistics.get(index);
        }

        @Override
        public int size() {
            return statistics.size();
        }

        @Override
        public Spliterator<GameStatistics> spliterator() {
            if (traversed) {
                throw new IllegalStateException("game statistics must be traversed only once");
            }
            traversed = true;
            return statistics.spliterator();
        }
    }
}
