package com.example.baseballorders.simulator.application;

import com.example.baseballorders.simulator.application.dto.SimulationResponse;
import com.example.baseballorders.simulator.domain.model.GameBattingContext;
import com.example.baseballorders.simulator.domain.model.behavior.AtBatBehavior;
import com.example.baseballorders.simulator.domain.model.player.LineUpEntity;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Simulates a baseball game using the configured batting behaviors. */
@Service
public class SimulateGameUseCase {

    private final Map<String, AtBatBehavior> behaviors;

    /**
     * Creates a game simulation use case.
     *
     * @param behaviors batting behaviors available to the simulation
     */
    public SimulateGameUseCase(Map<String, AtBatBehavior> behaviors) {
        this.behaviors = behaviors;
    }

    /**
     * Simulates a nine-player lineup until the game ends.
     *
     * @param lineUpEntity lineup used for the simulation
     * @return response containing the simulation score
     * @throws IllegalArgumentException when the lineup does not contain exactly nine batters
     */
    public SimulationResponse simulateGame(LineUpEntity lineUpEntity) {
        if (lineUpEntity.getBatterEntities().size() != 9) {
            throw new IllegalArgumentException("LineUpEntity size must be 9");
        }
        // 1. 試合開始前にIDを確定
        // 2. Stateパターンで試合実行（gameIdをコンテキストに保持）
        GameBattingContext ctx = new GameBattingContext(lineUpEntity);
        while (!ctx.isGameOver()) {
            ctx.nextAtBat();
        }

        return new SimulationResponse(Math.toIntExact(ctx.getTotalScore()), 4);
    }
}
