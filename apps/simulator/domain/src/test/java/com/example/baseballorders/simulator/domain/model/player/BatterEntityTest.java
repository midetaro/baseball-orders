package com.example.baseballorders.simulator.domain.model.player;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.baseballorders.simulator.domain.code.BattingResult;
import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.OutCount;
import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.model.behavior.StealStrategy;
import com.example.baseballorders.simulator.domain.model.state.SingleBasesState;
import com.example.baseballorders.simulator.domain.model.statistics.GameStatisticsRecorder;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BatterEntityTest {

    @Test
    @DisplayName("バントすると打者のバント成功率を戦略に渡して結果を返す")
    void delegatesBuntWithBatterSuccessRate() {
        // given
        var receivedRate = new AtomicReference<Float>();
        var batter =
                new BatterEntity(
                        0.3f,
                        0.4f,
                        0.75f,
                        0.85f,
                        (onBasePercentage, slugging) -> BattingResult.OUT,
                        new NeverStealStrategy(),
                        (successRate, outCounts, basesState) -> {
                            receivedRate.set(successRate);
                            return BuntResult.SUCCESS;
                        });

        var statisticsRecorder = new GameStatisticsRecorder();

        // when
        var observedBatter = batter.observedBy(statisticsRecorder);

        // when
        BuntResult result =
                observedBatter.bunt(OutCount.NO_OUT, new SingleBasesState(observedBatter));

        // then
        assertAll(
                () -> assertEquals(BuntResult.SUCCESS, result),
                () -> assertEquals(0.75f, receivedRate.get()),
                () -> assertEquals(1, statisticsRecorder.snapshot().buntCount()));
    }

    @Test
    @DisplayName("盗塁すると打者の盗塁成功率を戦略に渡して結果を返す")
    void delegatesStealWithBatterSuccessRate() {
        // given
        var receivedRate = new AtomicReference<Float>();
        StealStrategy strategy =
                new StealStrategy() {
                    @Override
                    public StealResult runToDouble(float successRate) {
                        receivedRate.set(successRate);
                        return StealResult.SUCCESS;
                    }

                    @Override
                    public StealResult runToTriple(float successRate) {
                        return StealResult.NOT_TRY;
                    }
                };
        var batter =
                new BatterEntity(
                        0.3f,
                        0.4f,
                        0.75f,
                        0.85f,
                        (onBasePercentage, slugging) -> BattingResult.OUT,
                        strategy,
                        (successRate, outCounts, basesState) -> BuntResult.SUCCESS);

        var statisticsRecorder = new GameStatisticsRecorder();

        // when
        StealResult result = batter.observedBy(statisticsRecorder).stealToDouble();

        // then
        assertAll(
                () -> assertEquals(StealResult.SUCCESS, result),
                () -> assertEquals(0.85f, receivedRate.get()),
                () -> assertEquals(1, statisticsRecorder.snapshot().stealCount()));
    }

    @Test
    @DisplayName("本塁打なら打撃前の走者数に応じた本塁打統計を記録する")
    void recordsHomeRunStatisticsWhenSwingHitsHomer() {
        // given
        var batter = batterWith(BattingResult.HIT_HOMER, BuntResult.NOT_TRY, StealResult.NOT_TRY);
        var statisticsRecorder = new GameStatisticsRecorder();

        // when
        BattingResult result = batter.observedBy(statisticsRecorder).swing(2);

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
        var batter = batterWith(BattingResult.OUT, BuntResult.FAILURE, StealResult.NOT_TRY);
        var statisticsRecorder = new GameStatisticsRecorder();

        // when
        var observedBatter = batter.observedBy(statisticsRecorder);
        observedBatter.swing(0);
        observedBatter.bunt(OutCount.NO_OUT, new SingleBasesState(observedBatter));
        observedBatter.stealToDouble();
        observedBatter.stealToTriple();

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
        var batter =
                batterWith(BattingResult.HIT_HOMER, BuntResult.NOT_TRY, StealResult.NOT_TRY)
                        .observedBy(statisticsRecorder);

        // when
        BattingResult result = batter.swing(1);

        // then
        assertAll(
                () -> assertEquals(BattingResult.HIT_HOMER, result),
                () -> assertEquals(1, statisticsRecorder.snapshot().homeRunCount()),
                () -> assertEquals(1, statisticsRecorder.snapshot().twoRunHomeRunCount()));
    }

    private static BatterEntity batterWith(
            BattingResult battingResult, BuntResult buntResult, StealResult stealResult) {
        return new BatterEntity(
                0.3f,
                0.4f,
                0.75f,
                0.85f,
                (onBasePercentage, slugging) -> battingResult,
                new FixedStealStrategy(stealResult),
                (successRate, outCounts, basesState) -> buntResult);
    }

    private static final class FixedStealStrategy implements StealStrategy {

        private final StealResult stealResult;

        private FixedStealStrategy(StealResult stealResult) {
            this.stealResult = stealResult;
        }

        @Override
        public StealResult runToDouble(float successRate) {
            return stealResult;
        }

        @Override
        public StealResult runToTriple(float successRate) {
            return stealResult;
        }
    }

    private static final class NeverStealStrategy implements StealStrategy {

        @Override
        public StealResult runToDouble(float successRate) {
            return StealResult.NOT_TRY;
        }

        @Override
        public StealResult runToTriple(float successRate) {
            return StealResult.NOT_TRY;
        }
    }
}
