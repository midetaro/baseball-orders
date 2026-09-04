package com.example.baseballorders.simulator.application;

import static org.junit.jupiter.api.Assertions.*;

import com.example.baseballorders.simulator.application.contract.SimulationResponse;
import com.example.baseballorders.simulator.application.usecase.SimulateGameUseCase;
import com.example.baseballorders.simulator.domain.model.behavior.AtBatBehavior;
import com.example.baseballorders.simulator.domain.model.behavior.NowayStealBehavior;
import com.example.baseballorders.simulator.domain.model.behavior.ShortDistanceAtBatBehavior;
import com.example.baseballorders.simulator.domain.model.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.player.LineUpEntity;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimulateGameUseCaseTest {

    ShortDistanceAtBatBehavior shortDistanceAtBatBehavior = new ShortDistanceAtBatBehavior();
    Map<String, AtBatBehavior> map = Map.of("shortDistanceAtBat", shortDistanceAtBatBehavior);

    SimulateGameUseCase simulateGameUseCase = new SimulateGameUseCase(map, 3);

    @DisplayName("9人の打順でシミュレーションを実行すると設定された試合数分の結果を返す")
    @Test
    public void invoke() {
        // given
        List<BatterEntity> batterEntities =
                IntStream.rangeClosed(1, 9)
                        .mapToObj(
                                number ->
                                        new BatterEntity(
                                                "batter" + number,
                                                0.4f,
                                                0.4f,
                                                0.7f,
                                                shortDistanceAtBatBehavior,
                                                new NowayStealBehavior(),
                                                successRate ->
                                                        com.example.baseballorders.simulator.domain
                                                                .code.BuntResult.SUCCESS))
                        .toList();
        // when
        List<SimulationResponse> results =
                simulateGameUseCase.invoke(new LineUpEntity(batterEntities));
        // then
        assertAll(
                () -> assertEquals(3, results.size(), "設定された3試合分の結果であること"),
                () ->
                        assertTrue(
                                results.stream().allMatch(result -> result.score() >= 0),
                                "すべての得点が0以上であること"),
                () ->
                        assertTrue(
                                results.stream().allMatch(result -> result.runs() == 4),
                                "すべての失点が設定されていること"));
    }
}
