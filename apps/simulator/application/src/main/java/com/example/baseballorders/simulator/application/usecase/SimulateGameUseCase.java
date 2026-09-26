package com.example.baseballorders.simulator.application.usecase;

import com.example.baseballorders.simulator.application.contract.SimulationResult;
import com.example.baseballorders.simulator.application.contract.SimulationResultBuilder;
import com.example.baseballorders.simulator.domain.game.BaseStateFactory;
import com.example.baseballorders.simulator.domain.game.GameBattingContext;
import com.example.baseballorders.simulator.domain.player.LineUpEntity;
import com.example.baseballorders.simulator.domain.statistics.GameTransitionRecorder;
import com.example.baseballorders.simulator.domain.statistics.ScoreAccumulator;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Simulates baseball games for a supplied lineup. */
@Service
public class SimulateGameUseCase {

    private final int gameCount;
    private final BaseStateFactory baseStateFactory;

    /**
     * Creates a game simulation use case.
     *
     * @param gameCount number of games to simulate for each request
     * @param baseStateFactory factory used to create base states during a game
     */
    @Autowired
    public SimulateGameUseCase(
            @Value("${simulation.game-count}") int gameCount, BaseStateFactory baseStateFactory) {
        this.gameCount = gameCount;
        this.baseStateFactory = baseStateFactory;
    }

    /**
     * Simulates the configured number of games with a nine-player lineup and calculates their score
     * statistics.
     *
     * <p>Equivalent to {@code invoke(lineUpEntity, SimulationRunMode.LARGE_SCALE_RUN)}.
     *
     * @param lineUpEntity lineup used for the simulation
     * @return each game response and aggregate score statistics
     * @throws IllegalArgumentException when the lineup does not contain exactly nine batters
     */
    public SimulationResult invoke(LineUpEntity lineUpEntity) {
        return invoke(lineUpEntity, SimulationRunMode.LARGE_SCALE_RUN);
    }

    /**
     * Simulates a nine-player lineup according to the requested run mode.
     *
     * <p>{@link SimulationRunMode#LARGE_SCALE_RUN} repeats the configured number of games and
     * returns only aggregate score statistics. {@link SimulationRunMode#SINGLE_GAME_RUN} runs
     * exactly one game regardless of the configured game count, and additionally returns the
     * chronological play-by-play transitions of that game.
     *
     * @param lineUpEntity lineup used for the simulation
     * @param mode run mode selecting a large-scale statistical run or a single detailed game run
     * @return aggregate score statistics, and play-by-play transitions when {@code mode} is {@link
     *     SimulationRunMode#SINGLE_GAME_RUN}
     * @throws IllegalArgumentException when the lineup does not contain exactly nine batters
     */
    public SimulationResult invoke(LineUpEntity lineUpEntity, SimulationRunMode mode) {
        if (lineUpEntity.getBatterEntities().size() != 9) {
            throw new IllegalArgumentException("LineUpEntity size must be 9");
        }
        return switch (mode) {
            case LARGE_SCALE_RUN -> simulateLargeScale(lineUpEntity);
            case SINGLE_GAME_RUN -> simulateSingleGame(lineUpEntity);
        };
    }

    private SimulationResult simulateLargeScale(LineUpEntity lineUpEntity) {
        IO.println("試合数：" + gameCount);
        ScoreAccumulator scoreAccumulator = new ScoreAccumulator();
        IntStream.range(0, gameCount).forEach(_ -> simulate(lineUpEntity, scoreAccumulator));
        return SimulationResultBuilder.simulationResult()
                .statistics(scoreAccumulator.toScoreStatistics())
                .transitions(List.of())
                .build();
    }

    private SimulationResult simulateSingleGame(LineUpEntity lineUpEntity) {
        ScoreAccumulator scoreAccumulator = new ScoreAccumulator();
        GameTransitionRecorder transitionRecorder = new GameTransitionRecorder();
        GameBattingContext ctx =
                new GameBattingContext(
                        lineUpEntity, scoreAccumulator, baseStateFactory, transitionRecorder);
        while (!ctx.isGameOver()) {
            ctx.nextAtBat();
        }
        return SimulationResultBuilder.simulationResult()
                .statistics(scoreAccumulator.toScoreStatistics())
                .transitions(transitionRecorder.snapshot())
                .build();
    }

    private void simulate(LineUpEntity lineUpEntity, ScoreAccumulator scoreAccumulator) {
        // 1. 試合開始前にIDを確定
        // 2. Stateパターンで試合実行（gameIdをコンテキストに保持）
        GameBattingContext ctx =
                new GameBattingContext(lineUpEntity, scoreAccumulator, baseStateFactory);
        while (!ctx.isGameOver()) {
            ctx.nextAtBat();
        }
    }
}
