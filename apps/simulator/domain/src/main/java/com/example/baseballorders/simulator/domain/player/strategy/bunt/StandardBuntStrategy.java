package com.example.baseballorders.simulator.domain.player.strategy.bunt;

import com.example.baseballorders.simulator.domain.play.BuntResult;
import com.example.baseballorders.simulator.domain.play.BuntType;
import com.example.baseballorders.simulator.domain.play.OutCount;
import com.example.baseballorders.simulator.domain.player.strategy.RandomGenerator;
import com.example.baseballorders.simulator.domain.rule.BuntProbabilities;
import lombok.RequiredArgsConstructor;

/** 設定された成功率に基づく標準的なバント戦略。 */
@RequiredArgsConstructor
public final class StandardBuntStrategy implements BuntStrategy {

    private final BuntProbabilities buntProbabilities;

    @Override
    public BuntResult bunt(OutCount outCount, BuntType buntType) {
        return switch (outCount) {
            case NO_OUT -> attempt(buntType);
            case ONE_OUT, TWO_OUT, THREE_OUT -> BuntResult.NOT_TRY;
        };
    }

    private BuntResult attempt(BuntType buntType) {
        return switch (buntType) {
            case ADVANCING -> roll(buntProbabilities.advancingSuccessRate());
            case SQUEEZE -> attemptSqueeze();
        };
    }

    private BuntResult attemptSqueeze() {
        if (RandomGenerator.nextFloat() >= buntProbabilities.squeezeChallengeRate()) {
            return BuntResult.NOT_TRY;
        }
        return roll(buntProbabilities.squeezeSuccessRate());
    }

    private BuntResult roll(float successRate) {
        return RandomGenerator.nextFloat() < successRate ? BuntResult.SUCCESS : BuntResult.FAILURE;
    }
}
