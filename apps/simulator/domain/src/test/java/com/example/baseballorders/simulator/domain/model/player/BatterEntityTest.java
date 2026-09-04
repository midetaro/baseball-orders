package com.example.baseballorders.simulator.domain.model.player;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.baseballorders.simulator.domain.code.BattingResult;
import com.example.baseballorders.simulator.domain.code.BuntResult;
import com.example.baseballorders.simulator.domain.code.StealResult;
import com.example.baseballorders.simulator.domain.model.behavior.StealStrategy;
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
                        (hitAverage, slugging) -> BattingResult.OUT,
                        new NeverStealStrategy(),
                        successRate -> BuntResult.SUCCESS);

        // when
        String name = batter.getName();
        float hitAverage = batter.getHitAverage();
        float sluggish = batter.getSluggish();
        float buntSuccessRate = batter.getBuntSuccessRate();

        // then
        assertAll(
                () -> assertEquals("batter", name),
                () -> assertEquals(0.3f, hitAverage),
                () -> assertEquals(0.4f, sluggish),
                () -> assertEquals(0.75f, buntSuccessRate));
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
                        (hitAverage, slugging) -> BattingResult.OUT,
                        new NeverStealStrategy(),
                        successRate -> {
                            receivedRate.set(successRate);
                            return BuntResult.SUCCESS;
                        });

        // when
        BuntResult result = batter.bunt();

        // then
        assertAll(
                () -> assertEquals(BuntResult.SUCCESS, result),
                () -> assertEquals(0.75f, receivedRate.get()));
    }

    private static final class NeverStealStrategy implements StealStrategy {

        @Override
        public StealResult runToDouble() {
            return StealResult.NOT_TRY;
        }

        @Override
        public StealResult runToTriple() {
            return StealResult.NOT_TRY;
        }
    }
}
