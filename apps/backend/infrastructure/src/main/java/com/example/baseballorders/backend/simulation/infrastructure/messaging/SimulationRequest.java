package com.example.baseballorders.backend.simulation.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * simulatorへ送信するSQSシミュレーション要求。
 *
 * @param simulationId シミュレーションを識別するID
 * @param players 打順どおりに並んだ9人の選手データ
 */
public record SimulationRequest(
        @JsonProperty("simulation_id") String simulationId, List<PlayerData> players) {}
