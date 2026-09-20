package com.example.baseballorders.simulator.domain.entity.behavior.steal;

import com.example.baseballorders.simulator.domain.code.StealResult;
import org.springframework.stereotype.Component;

@Component("nowayStealStrategy")
public final class NowayStealStrategy implements StealStrategy {

    @Override
    public StealResult runToDouble(float successRate) {
        return StealResult.NOT_TRY;
    }

    @Override
    public StealResult runToTriple(float successRate) {
        return StealResult.NOT_TRY;
    }
}
