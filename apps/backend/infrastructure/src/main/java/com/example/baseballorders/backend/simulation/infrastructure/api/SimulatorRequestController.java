package com.example.baseballorders.backend.simulation.infrastructure.api;

import com.example.baseballorders.backend.application.SimulationCoordinator;
import com.example.baseballorders.backend.simulation.domain.SimulationResult;
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
     * player IDを受け取り、SQS結果を受信するまでHTTP要求を待機して結果を返す。
     *
     * @param players 打順どおりに並んだ9人のplayer ID
     * @return simulatorから返されたシミュレーション結果
     */
    @PostMapping
    public SimulationResult send(@RequestBody List<PlayerIdRequest> players) {
        return coordinator.simulate(players.stream().map(PlayerIdRequest::playerId).toList());
    }
}
