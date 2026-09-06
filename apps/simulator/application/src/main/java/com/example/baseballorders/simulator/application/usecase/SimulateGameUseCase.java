package com.example.baseballorders.simulator.application.usecase;

import com.example.baseballorders.simulator.application.contract.SimulationResponse;
import com.example.baseballorders.simulator.application.contract.SimulationResult;
import com.example.baseballorders.simulator.domain.model.GameBattingContext;
import com.example.baseballorders.simulator.domain.model.behavior.AtBatBehavior;
import com.example.baseballorders.simulator.domain.model.player.LineUpEntity;
import com.example.baseballorders.simulator.domain.model.statistics.ScoreStatisticsCalculator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Simulates a baseball game using the configured batting behaviors. */
@Service
public class SimulateGameUseCase {

    private final Map<String, AtBatBehavior> behaviors;
    private final int gameCount;
    private final ScoreStatisticsCalculator scoreStatisticsCalculator;

    /**
     * Creates a game simulation use case.
     *
     * @param behaviors batting behaviors available to the simulation
     * @param gameCount number of games to simulate for each request
     */
    public SimulateGameUseCase(
            Map<String, AtBatBehavior> behaviors,
            @Value("${simulation.game-count}") int gameCount) {
        this.behaviors = behaviors;
        this.gameCount = gameCount;
        this.scoreStatisticsCalculator = new ScoreStatisticsCalculator();
    }

    /**
     * Simulates the configured number of games with a nine-player lineup and calculates their score
     * statistics.
     *
     * @param lineUpEntity lineup used for the simulation
     * @return each game response and aggregate score statistics
     * @throws IllegalArgumentException when the lineup does not contain exactly nine batters
     */
    public SimulationResult invoke(LineUpEntity lineUpEntity) {
        if (lineUpEntity.getBatterEntities().size() != 9) {
            throw new IllegalArgumentException("LineUpEntity size must be 9");
        }
        IO.println("試合数：" + gameCount);
        List<SimulationResponse> results =
                IntStream.range(0, gameCount).mapToObj(ignored -> simulate(lineUpEntity)).toList();
        return new SimulationResult(
                results,
                scoreStatisticsCalculator.calculate(
                        results.stream().map(SimulationResponse::score).toList()));
    }

    private SimulationResponse simulate(LineUpEntity lineUpEntity) {
        // 1. 試合開始前にIDを確定
        // 2. Stateパターンで試合実行（gameIdをコンテキストに保持）
        GameBattingContext ctx = new GameBattingContext(lineUpEntity);
        while (!ctx.isGameOver()) {
            ctx.nextAtBat();
        }

        return new SimulationResponse(Math.toIntExact(ctx.getTotalScore()), 4);
    }
}
