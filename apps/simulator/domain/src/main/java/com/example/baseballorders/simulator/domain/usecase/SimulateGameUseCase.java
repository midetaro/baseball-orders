package com.example.baseballorders.simulator.domain.usecase;

import com.example.baseballorders.simulator.domain.model.GameContext;
import com.example.baseballorders.simulator.domain.model.behavior.AtBatBehavior;
import com.example.baseballorders.simulator.domain.model.player.LineUpEntity;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class SimulateGameUseCase {

    private final Map<String, AtBatBehavior> behaviors;

    public GameContext simulateGame(LineUpEntity lineUpEntity) {
        if (lineUpEntity.getBatterEntities().size() != 9) {
            throw new IllegalArgumentException("LineUpEntity size must be 9");
        }
        // 1. 試合開始前にIDを確定
        // 2. Stateパターンで試合実行（gameIdをコンテキストに保持）
        GameContext ctx = new GameContext(lineUpEntity);
        while (!ctx.isGameOver()) {
            ctx.nextAtBat();
        }
        return ctx;
    }
}
