package com.example.baseballorders.backend.simulation.infrastructure.api;

import com.example.baseballorders.backend.simulation.infrastructure.messaging.PlayerData;
import com.example.baseballorders.backend.simulation.infrastructure.messaging.SimulatorRequestSender;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** シミュレーション要求をHTTP APIから受け付けるController。 */
@RestController
@RequestMapping("/simulations")
public final class SimulatorRequestController {

    private final SimulatorRequestSender sender;

    /**
     * simulatorへの送信機能を使用するControllerを作成する。
     *
     * @param sender simulatorへ選手データを送信する機能
     * @throws NullPointerException senderがnullの場合
     */
    public SimulatorRequestController(SimulatorRequestSender sender) {
        this.sender = Objects.requireNonNull(sender, "sender must not be null");
    }

    /**
     * API本文から9人の選手データを受け取り、simulatorへのメッセージとして送信する。
     *
     * @param players 打順どおりに並んだ9人の選手データ
     * @return SQS要求へ付与したsimulation ID
     * @throws NullPointerException playersまたはその要素がnullの場合
     * @throws IllegalArgumentException 選手データが9人分ではない場合
     */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public String send(@RequestBody List<PlayerData> players) {
        return sender.send(players);
    }
}
