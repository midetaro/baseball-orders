package com.example.baseballorders.simulator.domain.model.state;

import com.example.baseballorders.simulator.domain.code.Base;
import com.example.baseballorders.simulator.domain.model.GameBattingContext;
import com.example.baseballorders.simulator.domain.model.player.BatterEntity;

public final class FirstThirdBaseState extends BasesState implements StealableToDoubleBase {
    public FirstThirdBaseState() {
        this(null, null);
    }

    public FirstThirdBaseState(BatterEntity firstRunner, BatterEntity thirdRunner) {
        super(firstRunner, null, thirdRunner);
    }

    @Override
    public BatterEntity runnerOnFirst() {
        return runnerAt(Base.FIRST);
    }

    /**
     * Scores the runner on third, advances the runner on first, and places the batter on first.
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
        context.addScore(2);
        return advance(Base.THIRD).withRunnerAt(Base.THIRD, batterEntity);
    }

    @Override
    public BasesState hitHomer(GameBattingContext context, BatterEntity batterEntity) {
        context.addScore(3);
        return empty();
    }
}
