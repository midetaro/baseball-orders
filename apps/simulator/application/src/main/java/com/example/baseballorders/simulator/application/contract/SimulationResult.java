package com.example.baseballorders.simulator.application.contract;

import com.example.baseballorders.simulator.domain.statistics.GameTransition;
import com.example.baseballorders.simulator.domain.statistics.ScoreStatistics;
import java.util.List;
import org.jilt.Builder;
import org.jilt.BuilderStyle;

/**
 * Internal aggregate result for all games executed for one simulation request.
 *
 * @param statistics aggregate score and play statistics computed from every executed game
 * @param transitions chronological play-by-play transitions for a single-game run; empty for a
 *     large-scale run
 */
@Builder(style = BuilderStyle.STAGED)
public record SimulationResult(ScoreStatistics statistics, List<GameTransition> transitions) {

    /**
     * Creates a large-scale run result with no transitions, for compatibility with existing
     * callers.
     *
     * @param statistics aggregate score and play statistics computed from every executed game
     */
    public SimulationResult(ScoreStatistics statistics) {
        this(statistics, List.of());
    }
}
