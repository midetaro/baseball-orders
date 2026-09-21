package com.example.baseballorders.simulator.domain.player.strategy.bunt;

import com.example.baseballorders.simulator.domain.play.BuntResult;
import com.example.baseballorders.simulator.domain.play.OutCount;
import com.example.baseballorders.simulator.domain.player.strategy.RandomGenerator;

/** 標準戦略より広い試合状況でバントを試みる積極的な戦略。 */
public final class EagerBuntStrategy implements BuntStrategy {

    @Override
    public BuntResult bunt(float successRate, OutCount outCount) {
        return switch (outCount) {
            case NO_OUT, ONE_OUT -> attempt(successRate);
            case TWO_OUT, THREE_OUT -> BuntResult.NOT_TRY;
        };
    }

    private BuntResult attempt(float successRate) {
        return RandomGenerator.nextFloat() < successRate ? BuntResult.SUCCESS : BuntResult.FAILURE;
    }
}
