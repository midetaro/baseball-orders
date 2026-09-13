package com.example.baseballorders.simulator.domain.model.statistics;

import java.util.List;

/** Calculates score statistics from completed game scores. */
public final class ScoreStatisticsCalculator {

    /**
     * Calculates the average, median, and maximum score.
     *
     * @param scores scores from completed games
     * @return calculated score statistics
     * @throws IllegalArgumentException when {@code scores} is empty
     */
    public ScoreStatistics calculate(List<Integer> scores) {
        ScoreAccumulator accumulator = new ScoreAccumulator();
        for (int score : scores) {
            accumulator.add(score);
        }
        return accumulator.toScoreStatistics();
    }
}
