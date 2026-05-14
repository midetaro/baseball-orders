package org.example.domain.usecase;

import org.example.domain.model.behavior.ShortDistanceAtBatBehavior;
import org.example.domain.model.player.Batter;
import org.junit.jupiter.api.Test;

import java.util.List;
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
        var batters = List.of(
                new Batter("batter1", 0.3f, 0.4f, shortDistanceAtBatBehavior),
                new Batter("batter2", 0.4f, 0.5f, shortDistanceAtBatBehavior),
                new Batter("batter3", 0.5f, 0.6f, shortDistanceAtBatBehavior),
                new Batter("batter1", 0.3f, 0.6f, shortDistanceAtBatBehavior),
                new Batter("batter2", 0.4f, 0.5f, shortDistanceAtBatBehavior),
                new Batter("batter3", 0.5f, 0.3f, shortDistanceAtBatBehavior),
                new Batter("batter1", 0.3f, 0.6f, shortDistanceAtBatBehavior),
                new Batter("batter2", 0.4f, 0.4f, shortDistanceAtBatBehavior),
                new Batter("batter3", 0.5f, 0.5f, shortDistanceAtBatBehavior)
        );
        // when
        var result = gameSimulatorService.simulateGame(batters);
        // then
        assertAll(
                () -> assertTrue(result.isGameOver(), "ゲームが終了していること"),
                () -> assertEquals(9, result.getInning(), "イニングが9である")
        );
    }
}
