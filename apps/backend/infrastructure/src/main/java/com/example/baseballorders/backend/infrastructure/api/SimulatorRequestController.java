package com.example.baseballorders.backend.infrastructure.api;

import com.example.baseballorders.backend.application.SimulationCoordinator;
import com.example.baseballorders.backend.domain.PlayerData;
import com.example.baseballorders.backend.domain.PlayerDataBuilder;
import com.example.baseballorders.backend.domain.SimulationMode;
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
        return coordinator.simulate(toPlayerData(players));
    }

    /**
     * 画面で入力された打順データを受け取り、1試合実行モードでSQS結果を受信するまでHTTP要求を待機して結果を返す。
     *
     * @param players 打順どおりに並んだ9人の打撃データとバント・盗塁選択
     * @return simulatorから返された1試合分の状況推移を含むシミュレーション結果
     */
    @PostMapping("/single-game")
    public SimulationResult sendSingleGame(@RequestBody List<PlayerInputRequest> players) {
        return coordinator.simulate(toPlayerData(players), SimulationMode.SINGLE_GAME_RUN);
    }

    private List<PlayerData> toPlayerData(List<PlayerInputRequest> players) {
        return java.util.stream.IntStream.range(0, players.size())
                .mapToObj(
                        index -> {
                            var player = players.get(index);
                            return PlayerDataBuilder.playerData()
                                    .name((index + 1) + "番")
                                    .hitAverage(player.hitAverage())
                                    .sluggish(player.sluggish())
                                    .buntSuccessRate(player.buntSuccessRate())
                                    .buntEnabled(player.buntEnabled())
                                    .stealSuccessRate(player.stealSuccessRate())
                                    .stealEnabled(player.stealEnabled())
                                    .personality(player.personality())
                                    .build();
                        })
                .toList();
    }
}
