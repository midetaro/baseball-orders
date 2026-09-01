package com.example.baseballorders.backend.simulation.domain;

import java.util.UUID;

/**
 * backendがHTTP要求へ返すシミュレーション結果。
 *
 * @param simulationId シミュレーションの相関ID
 * @param score 得点
 * @param runs 失点
 */
public record SimulationResult(UUID simulationId, int score, int runs) {}
