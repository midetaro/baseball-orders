package com.example.baseballorders.simulator.application.contract;

import com.example.baseballorders.simulator.domain.statistics.GameStatistics;
import com.example.baseballorders.simulator.domain.statistics.GameStatisticsBuilder;
import org.jilt.Builder;
import org.jilt.BuilderStyle;

/**
 * Internal result of a completed game simulation.
 *
 * @param score 得点
 * @param runs 失点
 */
@Builder(style = BuilderStyle.STAGED)
public record SimulationResponse(int score, int runs, GameStatistics gameStatistics) {

    /**
     * Creates a response with no batting events for compatibility with score-only callers.
     *
     * @param score scored runs
     * @param runs allowed runs
     */
    public SimulationResponse(int score, int runs) {
        this(
                score,
                runs,
                GameStatisticsBuilder.gameStatistics()
                        .homeRunCount(0)
                        .soloHomeRunCount(0)
                        .twoRunHomeRunCount(0)
                        .threeRunHomeRunCount(0)
                        .grandSlamCount(0)
                        .buntCount(0)
                        .stealCount(0)
                        .buntFailureCount(0)
                        .stealFailureCount(0)
                        .advancingBuntCount(0)
                        .squeezeBuntCount(0)
                        .advancingBuntFailureCount(0)
                        .squeezeBuntFailureCount(0)
                        .stealToSecondCount(0)
                        .stealToThirdCount(0)
                        .build());
    }
}
