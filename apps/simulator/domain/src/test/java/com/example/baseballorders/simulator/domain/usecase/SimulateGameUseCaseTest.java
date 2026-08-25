package com.example.baseballorders.simulator.domain.usecase;

import com.example.baseballorders.simulator.domain.model.GameContext;
import com.example.baseballorders.simulator.domain.model.behavior.AtBatBehavior;
import com.example.baseballorders.simulator.domain.model.behavior.NowayStealBehavior;
import com.example.baseballorders.simulator.domain.model.behavior.ShortDistanceAtBatBehavior;
import com.example.baseballorders.simulator.domain.model.player.Batter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class SimulateGameUseCaseTest {

    ShortDistanceAtBatBehavior shortDistanceAtBatBehavior = new ShortDistanceAtBatBehavior();
    Map<String, AtBatBehavior> map = Map.of("shortDistanceAtBat", shortDistanceAtBatBehavior);

    SimulateGameUseCase simulateGameUseCase = new SimulateGameUseCase(
            map
    );

    @DisplayName("9人の打順でシミュレーションを実行すると9回で試合が終了する")
    @Test
    public void simulateGame() {
        // given
        List<Batter> batters = IntStream.rangeClosed(1, 9)
                .mapToObj(number -> new Batter(
                        "batter" + number,
                        0.4f,
                        0.4f,
                        shortDistanceAtBatBehavior,
                        new NowayStealBehavior()
                ))
                .toList();
        // when
        GameContext result = simulateGameUseCase.simulateGame(batters);
        // then
        System.out.println("得点：" + result.getTotalScore());
        assertAll(
                () -> assertTrue(result.isGameOver(), "ゲームが終了していること"),
                () -> assertEquals(9, result.getInning(), "イニングが9である")
        );
    }
}
