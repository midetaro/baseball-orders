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
        int stealFailureCount,
        int advancingBuntCount,
        int squeezeBuntCount,
        int advancingBuntFailureCount,
        int squeezeBuntFailureCount,
        int stealToSecondCount,
        int stealToThirdCount) {}
