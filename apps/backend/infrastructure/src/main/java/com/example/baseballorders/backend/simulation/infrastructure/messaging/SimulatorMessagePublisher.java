package com.example.baseballorders.backend.simulation.infrastructure.messaging;

import com.example.baseballorders.messaging.SimulationRequestMessage;

/** simulatorのメッセージチャネルへシミュレーション要求を発行するポート。 */
@FunctionalInterface
public interface SimulatorMessagePublisher {

    /**
     * 指定された打順をsimulatorのメッセージチャネルへ発行する。
     *
     * @param request シミュレーションIDと9人の選手データを含む要求
     */
    void publish(SimulationRequestMessage request);
}
