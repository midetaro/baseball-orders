package com.example.baseballorders.simulator.domain.model.statistics;

import java.util.Comparator;
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
        if (scores.isEmpty()) {
            throw new IllegalArgumentException("scores must not be empty");
        }
        List<Integer> sortedScores = scores.stream().sorted(Comparator.naturalOrder()).toList();
        int middleIndex = sortedScores.size() / 2;
        double median =
                sortedScores.size() % 2 == 0
                        ? (sortedScores.get(middleIndex - 1) + sortedScores.get(middleIndex)) / 2.0
                        : sortedScores.get(middleIndex);
        return new ScoreStatistics(
                scores.stream().mapToInt(Integer::intValue).average().orElseThrow(),
                median,
                sortedScores.getLast());
    }
}
