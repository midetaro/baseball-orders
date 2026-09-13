package com.example.baseballorders.simulator.domain.model.statistics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Accumulates scores and creates their aggregate statistics. */
final class ScoreAccumulator {
    private final List<Integer> scores = new ArrayList<>();
    private final Map<Integer, Integer> scoreDistribution = new TreeMap<>();
    private long totalScore;

    void add(int score) {
        scores.add(score);
        totalScore += score;
        scoreDistribution.merge(score, 1, Integer::sum);
    }

    ScoreStatistics toScoreStatistics() {
        if (scores.isEmpty()) {
            throw new IllegalArgumentException("scores must not be empty");
        }
        scores.sort(Integer::compareTo);
        int middleIndex = scores.size() / 2;
        double median =
                scores.size() % 2 == 0
                        ? (scores.get(middleIndex - 1) + scores.get(middleIndex)) / 2.0
                        : scores.get(middleIndex);
        return new ScoreStatistics(
                totalScore / (double) scores.size(),
                median,
                scores.getLast(),
                scores.size(),
                new LinkedHashMap<>(scoreDistribution),
                0,
                0,
                0,
                0,
                0,
                0,
                0);
    }
}
