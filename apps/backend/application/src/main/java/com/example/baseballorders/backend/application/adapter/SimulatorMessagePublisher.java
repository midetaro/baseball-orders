package com.example.baseballorders.backend.application.adapter;

import com.example.baseballorders.backend.application.dto.SimulationRequest;

/** simulatorのメッセージチャネルへシミュレーション要求を発行するポート。 */
@FunctionalInterface
public interface SimulatorMessagePublisher {

    /**
     * 指定された打順をsimulatorのメッセージチャネルへ発行する。
     *
     * @param request シミュレーションIDと9人の選手データを含む要求
     */
    void publish(SimulationRequest request);
}
