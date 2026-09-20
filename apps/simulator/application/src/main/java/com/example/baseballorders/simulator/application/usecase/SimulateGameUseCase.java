package com.example.baseballorders.simulator.application.usecase;

import com.example.baseballorders.simulator.application.contract.SimulationResult;
import com.example.baseballorders.simulator.domain.entity.player.LineUpEntity;
import com.example.baseballorders.simulator.domain.model.GameBattingContext;
import com.example.baseballorders.simulator.domain.model.base.BaseStateFactory;
import com.example.baseballorders.simulator.domain.model.statistics.ScoreAccumulator;
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
     * 指定された試合数でシミュレーション用ユースケースを作成する。
     *
     * @param gameCount リクエストごとに実行する試合数
     */
    public SimulateGameUseCase(int gameCount) {
        this(gameCount, new BaseStateFactory());
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
        ScoreAccumulator scoreAccumulator = new ScoreAccumulator();
        IntStream.range(0, gameCount).forEach(ignored -> simulate(lineUpEntity, scoreAccumulator));
        return new SimulationResult(scoreAccumulator.toScoreStatistics());
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
