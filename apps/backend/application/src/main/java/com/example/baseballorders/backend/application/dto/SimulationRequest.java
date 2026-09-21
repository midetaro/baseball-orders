package com.example.baseballorders.backend.application.dto;

import com.example.baseballorders.backend.domain.PlayerData;
import java.util.List;
import java.util.UUID;
import org.jilt.Builder;
import org.jilt.BuilderStyle;

/** simulatorへ送るbackend内部のシミュレーション要求。 */
@Builder(style = BuilderStyle.STAGED)
public record SimulationRequest(UUID simulationId, String version, List<PlayerData> players) {}
