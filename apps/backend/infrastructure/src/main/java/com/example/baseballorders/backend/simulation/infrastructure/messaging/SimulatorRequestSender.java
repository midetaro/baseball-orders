package com.example.baseballorders.backend.simulation.infrastructure.messaging;

import java.util.List;
import java.util.Objects;

/** APIからシミュレーション要求を受け取り、simulatorへメッセージを送信する。 */
public final class SimulatorRequestSender {

    private static final int LINEUP_SIZE = 9;

    private final SimulatorMessagePublisher publisher;

    /**
     * simulatorへのメッセージ発行処理を指定して送信機能を作成する。
     *
     * @param publisher simulatorへメッセージを発行する処理
     * @throws NullPointerException publisherがnullの場合
     */
    public SimulatorRequestSender(SimulatorMessagePublisher publisher) {
        this.publisher = Objects.requireNonNull(publisher, "publisher must not be null");
    }

    /**
     * APIから受け取った9人の選手データのスナップショットをsimulatorへ送信する。
     *
     * @param players 打順どおりに並んだ選手データ
     * @throws NullPointerException playersまたはその要素がnullの場合
     * @throws IllegalArgumentException 選手データが9人分ではない場合
     */
    public void send(List<PlayerData> players) {
        Objects.requireNonNull(players, "players must not be null");
        if (players.size() != LINEUP_SIZE) {
            throw new IllegalArgumentException("players must contain exactly 9 entries");
        }
        publisher.publish(List.copyOf(players));
    }
}
