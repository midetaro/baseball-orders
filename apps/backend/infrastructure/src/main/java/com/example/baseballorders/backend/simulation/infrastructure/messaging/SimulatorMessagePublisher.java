package com.example.baseballorders.backend.simulation.infrastructure.messaging;

import java.util.List;

/** simulatorのメッセージチャネルへシミュレーション要求を発行するポート。 */
@FunctionalInterface
public interface SimulatorMessagePublisher {

    /**
     * 指定された打順をsimulatorのメッセージチャネルへ発行する。
     *
     * @param players 打順どおりに並んだ9人の選手データ
     */
    void publish(List<PlayerData> players);
}
