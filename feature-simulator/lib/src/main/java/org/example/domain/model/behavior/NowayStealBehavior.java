package org.example.domain.model.behavior;

import org.example.domain.code.StealResult;
import org.springframework.stereotype.Component;

@Component("nowayStealBehavior")
public class NowayStealBehavior implements StealStrategy {

    @Override
    public StealResult runToDouble() {
        return StealResult.NOT_TRY;
    }

    @Override
    public StealResult runToTriple() {
        return StealResult.NOT_TRY;
    }
}
