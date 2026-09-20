package com.example.baseballorders.simulator.application.contract;

import com.example.baseballorders.simulator.domain.statistics.ScoreStatistics;
import org.jilt.Builder;
import org.jilt.BuilderStyle;

/** Internal aggregate result for all games executed for one simulation request. */
@Builder(style = BuilderStyle.STAGED)
public record SimulationResult(ScoreStatistics statistics) {}
