package com.example.baseballorders.simulator.domain.model.statistics;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScoreStatisticsCalculatorTest {

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
}
