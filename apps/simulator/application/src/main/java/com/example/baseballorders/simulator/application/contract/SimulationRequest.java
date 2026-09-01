package com.example.baseballorders.simulator.application.contract;

import java.util.List;

/** A game simulation request received from SQS. */
public record SimulationRequest(String gameId, String resultQueueUrl, List<PlayerData> players) {}
