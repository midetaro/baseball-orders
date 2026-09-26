package com.example.baseballorders.backend.application.dto;

import com.example.baseballorders.backend.domain.PlayerData;
import com.example.baseballorders.backend.domain.SimulationMode;
import java.util.List;
import java.util.UUID;
import org.jilt.Builder;
import org.jilt.BuilderStyle;

/**
 * simulatorへ送るbackend内部のシミュレーション要求。
 *
 * @param simulationId シミュレーションの相関ID
 * @param version メッセージスキーマのバージョン
 * @param players 打順どおりの9人の選手
 * @param mode 大規模実行と1試合実行を見分ける試合実行モード
 */
@Builder(style = BuilderStyle.STAGED)
public record SimulationRequest(
        UUID simulationId, String version, List<PlayerData> players, SimulationMode mode) {

    /** modeが未指定の場合は大規模実行へ補完する。 */
    public SimulationRequest {
        mode = mode == null ? SimulationMode.LARGE_SCALE_RUN : mode;
    }

    /**
     * Creates a large-scale run request for compatibility with existing callers.
     *
     * @param simulationId シミュレーションの相関ID
     * @param version メッセージスキーマのバージョン
     * @param players 打順どおりの9人の選手
     */
    public SimulationRequest(UUID simulationId, String version, List<PlayerData> players) {
        this(simulationId, version, players, SimulationMode.LARGE_SCALE_RUN);
    }
}
