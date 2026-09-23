package com.example.baseballorders.backend.application.dto;

import com.example.baseballorders.backend.domain.PitcherPersonality;
import com.example.baseballorders.backend.domain.PlayerData;
import java.util.List;
import java.util.UUID;
import org.jilt.Builder;
import org.jilt.BuilderStyle;

/** simulatorへ送るbackend内部のシミュレーション要求。 */
@Builder(style = BuilderStyle.STAGED)
public record SimulationRequest(
        UUID simulationId,
        String version,
        List<PlayerData> players,
        PitcherPersonality pitcherPersonality) {

    /**
     * Creates a request with the default pitcher personality for existing callers.
     *
     * @param simulationId simulation correlation ID
     * @param version message schema version
     * @param players players in batting order
     */
    public SimulationRequest(UUID simulationId, String version, List<PlayerData> players) {
        this(simulationId, version, players, PitcherPersonality.DEFAULT);
    }
}
