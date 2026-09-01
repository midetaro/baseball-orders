package com.example.baseballorders.backend.simulation.infrastructure.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * シミュレーションAPIで受け取る選手識別子。
 *
 * @param playerId 選手を識別するID
 */
public record PlayerIdRequest(@JsonProperty("player_id") Long playerId) {}
