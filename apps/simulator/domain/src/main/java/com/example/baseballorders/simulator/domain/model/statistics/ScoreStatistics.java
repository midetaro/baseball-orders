package com.example.baseballorders.simulator.domain.model.statistics;

/** Aggregated score statistics for a completed group of simulated games. */
public record ScoreStatistics(double averageScore, double medianScore, int maximumScore) {}
