package com.example.baseballorders.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

/**
 * backendからsimulatorへ送信する共有要求メッセージ。
 *
 * @param simulationId 相関に使用する一意なID
 * @param version メッセージスキーマのバージョン
 * @param players 打順どおりの9人の選手
 */
public record SimulationRequestMessage(
        @JsonProperty("simulation_id") UUID simulationId,
        String version,
        List<SimulationPlayerMessage> players) {}
