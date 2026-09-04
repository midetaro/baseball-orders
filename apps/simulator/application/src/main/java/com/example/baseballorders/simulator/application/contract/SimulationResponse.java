package com.example.baseballorders.simulator.application.contract;

/**
 * Internal result of a completed game simulation.
 *
 * @param score 得点
 * @param runs 失点
 */
public record SimulationResponse(int score, int runs) {}
