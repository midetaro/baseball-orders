package com.example.baseballorders.simulator.application.contract;

import com.example.baseballorders.simulator.domain.model.statistics.ScoreStatistics;
import java.util.List;

/** Internal aggregate result for all games executed for one simulation request. */
public record SimulationResult(List<SimulationResponse> results, ScoreStatistics statistics) {}
