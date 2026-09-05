package com.example.baseballorders.simulator.domain.model.player;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.baseballorders.simulator.domain.code.BattingResult;
import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.OutCount;
import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.model.behavior.StealStrategy;
import com.example.baseballorders.simulator.domain.model.state.SingleBasesState;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BatterEntityTest {

    @Test
    @DisplayName("打者の能力値を取得すると生成時の値を返す")
    void returnsBatterAttributes() {
        // given
        var batter =
                new BatterEntity(
                        "batter",
                        0.3f,
                        0.4f,
                        0.75f,
                        0.85f,
                        (hitAverage, slugging) -> BattingResult.OUT,
                        new NeverStealStrategy(),
                        (successRate, outCounts, basesState) -> BuntResult.SUCCESS);

        // when
        String name = batter.getName();
        float hitAverage = batter.getHitAverage();
        float sluggish = batter.getSluggish();
        float buntSuccessRate = batter.getBuntSuccessRate();
        float stealSuccessRate = batter.getStealSuccessRate();

        // then
        assertAll(
                () -> assertEquals("batter", name),
                () -> assertEquals(0.3f, hitAverage),
                () -> assertEquals(0.4f, sluggish),
                () -> assertEquals(0.75f, buntSuccessRate),
                () -> assertEquals(0.85f, stealSuccessRate));
    }

    @Test
    @DisplayName("バントすると打者のバント成功率を戦略に渡して結果を返す")
    void delegatesBuntWithBatterSuccessRate() {
        // given
        var receivedRate = new AtomicReference<Float>();
        var batter =
                new BatterEntity(
                        "batter",
                        0.3f,
                        0.4f,
                        0.75f,
                        0.85f,
                        (hitAverage, slugging) -> BattingResult.OUT,
                        new NeverStealStrategy(),
                        (successRate, outCounts, basesState) -> {
                            receivedRate.set(successRate);
                            return BuntResult.SUCCESS;
                        });

        // when
        BuntResult result = batter.bunt(OutCount.NO_OUT, new SingleBasesState());

        // then
        assertAll(
                () -> assertEquals(BuntResult.SUCCESS, result),
                () -> assertEquals(0.75f, receivedRate.get()));
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
                        "batter",
                        0.3f,
                        0.4f,
                        0.75f,
                        0.85f,
                        (hitAverage, slugging) -> BattingResult.OUT,
                        strategy,
                        (successRate, outCounts, basesState) -> BuntResult.SUCCESS);

        // when
        StealResult result = batter.stealToDouble();

        // then
        assertAll(
                () -> assertEquals(StealResult.SUCCESS, result),
                () -> assertEquals(0.85f, receivedRate.get()));
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
