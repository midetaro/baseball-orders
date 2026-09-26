package com.example.baseballorders.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jilt.Builder;
import org.jilt.BuilderStyle;

/**
 * backendからsimulatorへ送信する共有要求メッセージ。
 *
 * @param simulationId 相関に使用する一意なID
 * @param version メッセージスキーマのバージョン
 * @param players 打順どおりの9人の選手
 * @param pitcherPersonality 対戦する投手の性格
 * @param mode 大規模実行と1試合実行を見分ける試合実行モード
 */
@Builder(style = BuilderStyle.STAGED)
public record SimulationRequestMessage(
        @JsonProperty("simulation_id") UUID simulationId,
        String version,
        List<SimulationPlayerMessage> players,
        PitcherPersonality pitcherPersonality,
        SimulationMode mode) {

    /**
     * Creates a large-scale run request with the default pitcher personality for compatibility with
     * existing callers.
     *
     * @param simulationId 相関に使用する一意なID
     * @param version メッセージスキーマのバージョン
     * @param players 打順どおりの9人の選手
     */
    public SimulationRequestMessage(
            UUID simulationId, String version, List<SimulationPlayerMessage> players) {
        this(
                simulationId,
                version,
                players,
                PitcherPersonality.DEFAULT,
                SimulationMode.LARGE_SCALE_RUN);
    }

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
        pitcherPersonality =
                pitcherPersonality == null ? PitcherPersonality.DEFAULT : pitcherPersonality;
        mode = mode == null ? SimulationMode.LARGE_SCALE_RUN : mode;
    }
}
