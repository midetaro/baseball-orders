package com.example.baseballorders.simulator.application.dto;

/**
 * Result of a completed game simulation.
 *
 * @param score 得点
 * @param runs 失点
 */
public record SimulationResponse(int score, int runs) {}
