package com.example.baseballorders.simulator.application;

import lombok.AllArgsConstructor;
import com.example.baseballorders.simulator.domain.model.GameContext;
import com.example.baseballorders.simulator.domain.model.behavior.AtBatBehavior;
import com.example.baseballorders.simulator.domain.model.player.Batter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class SimulateGameUseCase {

    private final Map<String, AtBatBehavior> behaviors;

    public GameContext simulateGame(List<Batter> batters) {
        if (batters.size() != 9) {
            throw new IllegalArgumentException("Batters size must be 9");
        }
        // 1. 試合開始前にIDを確定

        // 2. Stateパターンで試合実行（gameIdをコンテキストに保持）
        GameContext ctx = new GameContext(batters);
        while (!ctx.isGameOver()) {
            ctx.nextAtBat();
        }
        return ctx;
    }
}
