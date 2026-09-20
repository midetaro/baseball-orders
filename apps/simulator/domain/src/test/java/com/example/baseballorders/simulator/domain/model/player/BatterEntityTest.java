package com.example.baseballorders.simulator.domain.model.player;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

import com.example.baseballorders.simulator.domain.code.BattingResult;
import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.OutCount;
import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.entity.behavior.BehaviorStrategies;
import com.example.baseballorders.simulator.domain.entity.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.statistics.GameStatisticsRecorder;
import com.example.baseballorders.simulator.domain.util.RandomGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class BatterEntityTest {

    @Test
    @DisplayName("バントすると打者のバント成功率を戦略に渡して結果を返す")
    void delegatesBuntWithBatterSuccessRate() {
        // given
        var batter =
                new BatterEntity(
                        0.3f,
                        0.4f,
                        0.75f,
                        0.85f,
                        BehaviorStrategies.middleDistanceHittingStrategy(),
                        BehaviorStrategies.noSteal(),
                        BehaviorStrategies.standardBunt());

        var statisticsRecorder = new GameStatisticsRecorder();

        // when
        var observedBatter = batter.observedBy(statisticsRecorder);

        // when
        BuntResult result;
        try (MockedStatic<RandomGenerator> randomGenerator = mockStatic(RandomGenerator.class)) {
            randomGenerator.when(RandomGenerator::nextFloat).thenReturn(0.1f);
            result = observedBatter.bunt(OutCount.NO_OUT);
        }

        // then
        assertAll(
                () -> assertEquals(BuntResult.SUCCESS, result),
                () -> assertEquals(1, statisticsRecorder.snapshot().buntCount()));
    }

    @Test
    @DisplayName("盗塁すると打者の盗塁成功率を戦略に渡して結果を返す")
    void delegatesStealWithBatterSuccessRate() {
        // given
        var batter =
                new BatterEntity(
                        0.3f,
                        0.4f,
                        0.75f,
                        0.85f,
                        BehaviorStrategies.middleDistanceHittingStrategy(),
                        BehaviorStrategies.eagerSteal(),
                        BehaviorStrategies.standardBunt());

        var statisticsRecorder = new GameStatisticsRecorder();

        // when
        StealResult result;
        try (MockedStatic<RandomGenerator> randomGenerator = mockStatic(RandomGenerator.class)) {
            randomGenerator.when(RandomGenerator::nextFloat).thenReturn(0.8f);
            result = batter.observedBy(statisticsRecorder).stealToDouble();
        }

        // then
        assertAll(
                () -> assertEquals(StealResult.SUCCESS, result),
                () -> assertEquals(1, statisticsRecorder.snapshot().stealCount()));
    }

    @Test
    @DisplayName("本塁打なら打撃前の走者数に応じた本塁打統計を記録する")
    void recordsHomeRunStatisticsWhenSwingHitsHomer() {
        // given
        var batter = homeRunBatter();
        var statisticsRecorder = new GameStatisticsRecorder();

        // when
        BattingResult result;
        try (MockedStatic<RandomGenerator> randomGenerator = mockStatic(RandomGenerator.class)) {
            randomGenerator.when(RandomGenerator::nextFloat).thenReturn(0.23f);
            result = batter.observedBy(statisticsRecorder).swing(2);
        }

        // then
        assertAll(
                () -> assertEquals(BattingResult.HIT_HOMER, result),
                () -> assertEquals(1, statisticsRecorder.snapshot().homeRunCount()),
                () -> assertEquals(1, statisticsRecorder.snapshot().threeRunHomeRunCount()));
    }

    @Test
    @DisplayName("成功しないプレーは統計を記録しない")
    void doesNotRecordStatisticsForUnsuccessfulPlays() {
        // given
        var batter =
                new BatterEntity(
                        0.3f,
                        0.4f,
                        0.75f,
                        0.85f,
                        BehaviorStrategies.middleDistanceHittingStrategy(),
                        BehaviorStrategies.noSteal(),
                        BehaviorStrategies.standardBunt());
        var statisticsRecorder = new GameStatisticsRecorder();

        // when
        try (MockedStatic<RandomGenerator> randomGenerator = mockStatic(RandomGenerator.class)) {
            randomGenerator.when(RandomGenerator::nextFloat).thenReturn(0.99f, 0.99f);
            var observedBatter = batter.observedBy(statisticsRecorder);
            observedBatter.swing(0);
            observedBatter.bunt(OutCount.NO_OUT);
            observedBatter.stealToDouble();
            observedBatter.stealToTriple();
        }

        // then
        assertAll(
                () -> assertEquals(0, statisticsRecorder.snapshot().homeRunCount()),
                () -> assertEquals(0, statisticsRecorder.snapshot().buntCount()),
                () -> assertEquals(0, statisticsRecorder.snapshot().stealCount()));
    }

    @Test
    @DisplayName("購読者へ打撃結果を通知する")
    void notifiesObserverOfBattingResult() {
        // given
        var statisticsRecorder = new GameStatisticsRecorder();
        var batter = homeRunBatter().observedBy(statisticsRecorder);

        // when
        BattingResult result;
        try (MockedStatic<RandomGenerator> randomGenerator = mockStatic(RandomGenerator.class)) {
            randomGenerator.when(RandomGenerator::nextFloat).thenReturn(0.23f);
            result = batter.swing(1);
        }

        // then
        assertAll(
                () -> assertEquals(BattingResult.HIT_HOMER, result),
                () -> assertEquals(1, statisticsRecorder.snapshot().homeRunCount()),
                () -> assertEquals(1, statisticsRecorder.snapshot().twoRunHomeRunCount()));
    }

    private static BatterEntity homeRunBatter() {
        return new BatterEntity(
                0.3f,
                0.4f,
                0.75f,
                0.85f,
                BehaviorStrategies.longDistanceAtBat(),
                BehaviorStrategies.noSteal(),
                BehaviorStrategies.noBunt());
    }
}
