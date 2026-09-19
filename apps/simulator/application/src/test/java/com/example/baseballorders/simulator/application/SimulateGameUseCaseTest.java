package com.example.baseballorders.simulator.application;

import static org.junit.jupiter.api.Assertions.*;

import com.example.baseballorders.simulator.application.contract.SimulationResult;
import com.example.baseballorders.simulator.application.usecase.SimulateGameUseCase;
import com.example.baseballorders.simulator.domain.model.behavior.BehaviorStrategies;
import com.example.baseballorders.simulator.domain.model.behavior.batting.AtBatBehavior;
import com.example.baseballorders.simulator.domain.model.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.player.LineUpEntity;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimulateGameUseCaseTest {

    AtBatBehavior atBatBehavior = BehaviorStrategies.middleDistanceAtBat();

    SimulateGameUseCase simulateGameUseCase = new SimulateGameUseCase(3);

    @DisplayName("9人の打順でシミュレーションを実行すると表示用の集計統計を返す")
    @Test
    public void invoke() {
        // given
        List<BatterEntity> batterEntities =
                IntStream.rangeClosed(1, 9)
                        .mapToObj(
                                number ->
                                        new BatterEntity(
                                                0.4f,
                                                0.4f,
                                                0.7f,
                                                0.8f,
                                                atBatBehavior,
                                                BehaviorStrategies.noSteal(),
                                                BehaviorStrategies.standardBunt()))
                        .toList();
        // when
        SimulationResult result = simulateGameUseCase.invoke(new LineUpEntity(batterEntities));
        // then
        assertAll(
                () -> assertEquals(3, result.statistics().gameCount(), "設定された3試合であること"),
                () ->
                        assertTrue(
                                result.statistics().scoreDistribution().entrySet().stream()
                                        .allMatch(entry -> entry.getKey() >= 0),
                                "分布上のすべての得点が0以上であること"),
                () ->
                        assertEquals(
                                3,
                                result.statistics().scoreDistribution().values().stream()
                                        .mapToInt(Integer::intValue)
                                        .sum(),
                                "得点分布の合計は試合数と一致すること"),
                () ->
                        assertEquals(
                                result.statistics().maximumScore(),
                                result.statistics().scoreDistribution().keySet().stream()
                                        .mapToInt(Integer::intValue)
                                        .max()
                                        .orElseThrow(),
                                "最大得点が全試合の結果から計算されること"),
                () ->
                        assertEquals(
                                result.statistics().homeRunCount(),
                                result.statistics().soloHomeRunCount()
                                        + result.statistics().twoRunHomeRunCount()
                                        + result.statistics().threeRunHomeRunCount()
                                        + result.statistics().grandSlamCount(),
                                "本塁打数は全試合の内訳の合計であること"),
                () -> assertTrue(result.statistics().buntCount() >= 0, "成功バント数は0以上であること"),
                () -> assertTrue(result.statistics().stealCount() >= 0, "成功盗塁数は0以上であること"));
    }
}
