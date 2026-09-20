package com.example.baseballorders.simulator.domain.statistics;

import java.util.Map;
import org.jilt.Builder;
import org.jilt.BuilderStyle;

/** Aggregated score statistics for a completed group of simulated games. */
@Builder(style = BuilderStyle.STAGED)
public record ScoreStatistics(
        double averageScore,
        double medianScore,
        int maximumScore,
        int gameCount,
        Map<Integer, Integer> scoreDistribution,
        int homeRunCount,
        int soloHomeRunCount,
        int twoRunHomeRunCount,
        int threeRunHomeRunCount,
        int grandSlamCount,
        int buntCount,
        int stealCount,
        int buntFailureCount,
        int stealFailureCount) {

    /**
     * Creates score-only statistics with no recorded batting events.
     *
     * @param averageScore average score
     * @param medianScore median score
     * @param maximumScore maximum score
     */
    public ScoreStatistics(double averageScore, double medianScore, int maximumScore) {
        this(averageScore, medianScore, maximumScore, 0, Map.of(), 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
