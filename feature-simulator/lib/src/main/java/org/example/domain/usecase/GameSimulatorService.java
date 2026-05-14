package org.example.domain.usecase;

import org.example.domain.model.GameContext;
import org.example.domain.model.player.Batter;

import java.util.List;

public class GameSimulatorService {

    public GameContext simulateGame(List<Batter> batters) {
        // 1. 試合開始前にIDを確定

        // 2. Stateパターンで試合実行（gameIdをコンテキストに保持）
        GameContext ctx = new GameContext(batters);
        while (!ctx.isGameOver()) {
            ctx.nextAtBat();
        }
        return ctx;
    }
}
