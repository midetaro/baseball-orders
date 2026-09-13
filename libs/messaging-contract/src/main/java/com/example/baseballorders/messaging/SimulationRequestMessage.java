package com.example.baseballorders.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
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
        List<SimulationPlayerMessage> players) {

    /**
     * Requires correlation metadata and a non-null player list, preserving its order in an
     * immutable copy.
     *
     * @throws NullPointerException when metadata, players, or any player is null
     */
    public SimulationRequestMessage {
        Objects.requireNonNull(simulationId, "simulationId must not be null");
        Objects.requireNonNull(version, "version must not be null");
        players = List.copyOf(Objects.requireNonNull(players, "players must not be null"));
    }
}
