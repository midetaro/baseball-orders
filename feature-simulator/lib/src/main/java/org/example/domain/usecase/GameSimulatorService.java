package org.example.domain.usecase;

import org.example.domain.model.GameContext;
import org.example.domain.model.player.Batter;

import java.util.List;

public class GameSimulatorService {

    public void simulateGame(List<Batter> batters) {
        // 1. 試合開始前にIDを確定

        // 2. Stateパターンで試合実行（gameIdをコンテキストに保持）
        GameContext ctx = new GameContext(batters);
        while (!ctx.isGameOver()) {
            ctx.nextAtBat();
        }

        // 3. 試合結果を非同期、または一括でDB保存
//        gameRepository.save(ctx.toEntity());

//        return ctx.toResponse();
    }
}
