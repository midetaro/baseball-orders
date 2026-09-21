package com.example.baseballorders.simulator.application.contract;

import com.example.baseballorders.simulator.domain.statistics.GameStatistics;
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
        this(score, runs, new GameStatistics(0, 0, 0, 0, 0, 0, 0, 0, 0));
    }
}
