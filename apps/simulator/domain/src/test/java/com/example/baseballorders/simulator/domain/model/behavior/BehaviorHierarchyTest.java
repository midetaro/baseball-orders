package com.example.baseballorders.simulator.domain.model.behavior;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BehaviorHierarchyTest {

    @Test
    @DisplayName("打撃・盗塁・バントの戦略階層をsealedにする")
    void sealsBehaviorStrategies() {
        // given

        // when
        var atBatBehaviorSealed = AtBatBehavior.class.isSealed();
        var stealStrategySealed = StealStrategy.class.isSealed();
        var buntStrategySealed = BuntStrategy.class.isSealed();

        // then
        assertAll(
                () -> assertTrue(atBatBehaviorSealed),
                () -> assertTrue(stealStrategySealed),
                () -> assertTrue(buntStrategySealed));
    }
}
