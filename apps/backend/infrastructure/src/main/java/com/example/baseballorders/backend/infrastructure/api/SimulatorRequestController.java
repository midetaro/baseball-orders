package com.example.baseballorders.backend.infrastructure.api;

import com.example.baseballorders.backend.application.SimulationCoordinator;
import com.example.baseballorders.backend.domain.PlayerData;
import com.example.baseballorders.backend.domain.SimulationResult;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** シミュレーション要求を同期HTTP APIとして受け付けるController。 */
@RestController
@RequestMapping("/simulations")
@RequiredArgsConstructor
public final class SimulatorRequestController {

    @NonNull private final SimulationCoordinator coordinator;

    /**
     * 画面で入力された打順データを受け取り、SQS結果を受信するまでHTTP要求を待機して結果を返す。
     *
     * @param players 打順どおりに並んだ9人の打撃データとバント・盗塁選択
     * @return simulatorから返されたシミュレーション結果
     */
    @PostMapping
    public SimulationResult send(@RequestBody List<PlayerInputRequest> players) {
        return coordinator.simulate(
                java.util.stream.IntStream.range(0, players.size())
                        .mapToObj(
                                index -> {
                                    var player = players.get(index);
                                    return new PlayerData(
                                            (index + 1) + "番",
                                            player.hitAverage(),
                                            player.sluggish(),
                                            player.buntSuccessRate(),
                                            player.buntEnabled(),
                                            player.stealSuccessRate(),
                                            player.stealEnabled());
                                })
                        .toList());
    }
}
