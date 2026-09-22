package com.example.baseballorders.simulator.domain.game;

import com.example.baseballorders.simulator.domain.play.OutCount;
import com.example.baseballorders.simulator.domain.player.BatterEntity;
import com.example.baseballorders.simulator.domain.player.LineUpEntity;
import com.example.baseballorders.simulator.domain.player.strategy.BehaviorStrategies;
import java.util.Collections;

/** 実物のイベントを通して、テスト用の走者配置を作る。 */
public final class GameStateTestFixture {
    private GameStateTestFixture() {}

    public static GameBattingContext context(
            BatterEntity first, BatterEntity second, BatterEntity third, OutCount outs) {
        var batter =
                new BatterEntity(
                        0,
                        0,
                        0,
                        0,
                        BehaviorStrategies.middleDistanceHittingStrategy(),
                        BehaviorStrategies.noSteal(),
                        BehaviorStrategies.noBunt());
        var context = new GameBattingContext(new LineUpEntity(Collections.nCopies(9, batter)));
        int mask = (first == null ? 0 : 1) | (second == null ? 0 : 2) | (third == null ? 0 : 4);
        switch (mask) {
            case 0 -> {}
            case 1 -> context.inningStateContext().currentBaseState().hitSingle(first);
            case 2 -> context.inningStateContext().currentBaseState().hitDouble(second);
            case 3 -> {
                context.inningStateContext().currentBaseState().hitSingle(second);
                context.inningStateContext().currentBaseState().hitSingle(first);
            }
            case 4 -> context.inningStateContext().currentBaseState().hitTriple(third);
            case 5 -> {
                context.inningStateContext().currentBaseState().hitDouble(third);
                context.inningStateContext().currentBaseState().hitSingle(first);
            }
            case 6 -> {
                context.inningStateContext().currentBaseState().hitSingle(third);
                context.inningStateContext().currentBaseState().hitDouble(second);
            }
            case 7 -> {
                context.inningStateContext().currentBaseState().hitSingle(third);
                context.inningStateContext().currentBaseState().hitSingle(second);
                context.inningStateContext().currentBaseState().hitSingle(first);
            }
            default -> throw new IllegalArgumentException();
        }
        int count =
                switch (outs) {
                    case NO_OUT -> 0;
                    case ONE_OUT -> 1;
                    case TWO_OUT -> 2;
                    case THREE_OUT -> throw new IllegalArgumentException("三死はイニング終了時にリセットされます");
                };
        for (int i = 0; i < count; i++) {
            context.inningStateContext().currentBaseState().out();
        }
        return context;
    }
}
