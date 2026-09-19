package com.example.baseballorders.simulator.domain.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

import com.example.baseballorders.simulator.domain.model.behavior.BehaviorStrategies;
import com.example.baseballorders.simulator.domain.model.player.BatterEntity;
import com.example.baseballorders.simulator.domain.model.player.LineUpEntity;
import com.example.baseballorders.simulator.domain.model.statistics.GameStatistics;
import com.example.baseballorders.simulator.domain.util.RandomGenerator;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class GameBattingContextTest {

    @Test
    @DisplayName("試合終了時に最終得点と統計を一度だけObserverへ通知する")
    void notifiesGameCompletionObserverOnlyOnce() {
        // given
        AtomicInteger notificationCount = new AtomicInteger();
        AtomicLong observedScore = new AtomicLong();
        AtomicReference<GameStatistics> observedStatistics = new AtomicReference<>();
        var context =
                new GameBattingContext(
                        new LineUpEntity(Collections.nCopies(9, battingOutBatter())),
                        (totalScore, gameStatistics) -> {
                            notificationCount.incrementAndGet();
                            observedScore.set(totalScore);
                            observedStatistics.set(gameStatistics);
                        });

        // when
        for (int inning = 0; inning < 8; inning++) {
            context.addOutCounts(3);
        }
        context.addOutCounts(3);
        context.addOutCounts(3);

        // then
        assertAll(
                () -> assertEquals(1, notificationCount.get()),
                () -> assertEquals(0, observedScore.get()),
                () -> assertEquals(0, observedStatistics.get().homeRunCount()),
                () -> assertEquals(0, observedStatistics.get().buntCount()),
                () -> assertEquals(0, observedStatistics.get().stealCount()));
    }

    @Test
    @DisplayName("打者のプレー結果を試合ごとの統計購読者へ通知する")
    void recordsBatterResultThroughGameObserver() {
        // given
        var batter =
                new BatterEntity(
                        0.3f,
                        0.4f,
                        0.7f,
                        0.8f,
                        BehaviorStrategies.longDistanceAtBat(),
                        BehaviorStrategies.noSteal(),
                        BehaviorStrategies.noBunt());
        var context = new GameBattingContext(new LineUpEntity(Collections.nCopies(9, batter)));

        // when
        try (MockedStatic<RandomGenerator> randomGenerator = mockStatic(RandomGenerator.class)) {
            randomGenerator.when(RandomGenerator::nextFloat).thenReturn(0.23f);
            context.nextAtBat();
        }

        // then
        assertAll(
                () -> assertEquals(1, context.getGameStatistics().homeRunCount()),
                () -> assertEquals(1, context.getGameStatistics().soloHomeRunCount()));
    }

    private static BatterEntity battingOutBatter() {
        return new BatterEntity(
                0.3f,
                0.4f,
                0.7f,
                0.8f,
                BehaviorStrategies.middleDistanceAtBat(),
                BehaviorStrategies.noSteal(),
                BehaviorStrategies.noBunt());
    }
}
