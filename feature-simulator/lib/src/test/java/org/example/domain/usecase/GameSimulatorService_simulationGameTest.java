package org.example.domain.usecase;

import org.example.domain.mock.Mock_Batters;
import org.example.domain.model.behavior.ShortDistanceAtBatBehavior;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class GameSimulatorService_simulationGameTest {

    ShortDistanceAtBatBehavior shortDistanceAtBatBehavior = new ShortDistanceAtBatBehavior();

    GameSimulatorService gameSimulatorService = new GameSimulatorService(
            Map.of("shortDistanceAtBat", shortDistanceAtBatBehavior)
    );

    @Test
    public void simulateGame() {
        // given
        var batters = Mock_Batters.mock();
        // when
        var result = gameSimulatorService.simulateGame(batters);
        // then
        System.out.println("得点：" + result.getTotalScore());
        assertAll(
                () -> assertTrue(result.isGameOver(), "ゲームが終了していること"),
                () -> assertEquals(9, result.getInning(), "イニングが9である")
        );
    }
}
