package org.example.domain.usecase;

import lombok.AllArgsConstructor;
import org.example.domain.model.GameContext;
import org.example.domain.model.behavior.AtBatBehavior;
import org.example.domain.model.player.Batter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class GameSimulatorService {

    private final Map<String, AtBatBehavior> behaviors;

    public GameContext simulateGame(List<Batter> batters) {
        if (batters.size() != 9) {
            throw new IllegalArgumentException("Batters size must be 9");
        }

        // 2. Stateパターンで試合実行（gameIdをコンテキストに保持）
        GameContext ctx = new GameContext(batters);
        while (!ctx.isGameOver()) {
            ctx.nextAtBat();
        }
        return ctx;
    }
}
