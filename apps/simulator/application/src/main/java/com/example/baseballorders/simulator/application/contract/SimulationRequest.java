package com.example.baseballorders.simulator.application.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * A game simulation request received from SQS.
 *
 * @param simulationId the identifier of the simulation
 * @param gameId the identifier of the game to simulate
 * @param resultQueueUrl the SQS queue URL to which the result is sent
 * @param players the players participating in the simulation
 */
public record SimulationRequest(
        @JsonProperty("simulation_id") String simulationId,
        String gameId,
        String resultQueueUrl,
        List<PlayerData> players) {}
