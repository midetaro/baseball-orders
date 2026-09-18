package com.example.baseballorders.simulator.domain.model.state;

import com.example.baseballorders.simulator.domain.code.Base;
import com.example.baseballorders.simulator.domain.model.GameBattingContext;
import com.example.baseballorders.simulator.domain.model.player.BatterEntity;

public final class ThirdBaseState extends BasesState {
    public ThirdBaseState() {
        this(null);
    }

    public ThirdBaseState(BatterEntity thirdRunner) {
        super(null, null, thirdRunner);
    }

    /**
     * Scores the runner on third and places the batter on first.
     *
     * @param context game context to update
     * @param batterEntity batter who hit the single
     */
    @Override
    public BasesState hitSingle(GameBattingContext context, BatterEntity batterEntity) {
        context.addScore(1);
        return advance(Base.FIRST).withRunnerAt(Base.FIRST, batterEntity);
    }

    @Override
    public BasesState hitDouble(GameBattingContext context, BatterEntity batterEntity) {
        context.addScore(1);
        return advance(Base.SECOND).withRunnerAt(Base.SECOND, batterEntity);
    }

    @Override
    public BasesState hitTriple(GameBattingContext context, BatterEntity batterEntity) {
        context.addScore(1);
        return advance(Base.THIRD).withRunnerAt(Base.THIRD, batterEntity);
    }

    @Override
    public BasesState hitHomer(GameBattingContext context, BatterEntity batterEntity) {
        context.addScore(2);
        return empty();
    }
}
