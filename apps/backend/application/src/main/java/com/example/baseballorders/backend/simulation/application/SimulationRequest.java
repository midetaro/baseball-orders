package com.example.baseballorders.backend.simulation.application;

import com.example.baseballorders.backend.simulation.domain.PlayerData;
import java.util.List;
import java.util.UUID;

/** simulatorへ送るbackend内部のシミュレーション要求。 */
public record SimulationRequest(UUID simulationId, String version, List<PlayerData> players) {}
