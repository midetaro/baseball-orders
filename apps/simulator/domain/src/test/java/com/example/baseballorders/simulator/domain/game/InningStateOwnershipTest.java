package com.example.baseballorders.simulator.domain.game;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InningStateOwnershipTest {
    @Test
    @DisplayName("イニングContextが現在の塁Stateだけを保持し打席処理の更新先になる")
    void ownsOnlyCurrentBaseStateAndReceivesAtBatUpdates() throws Exception {
        // given
        var inningContext = Class.forName(getClass().getPackageName() + ".InningStateContext");

        // when
        var gameFields = Arrays.asList(GameBattingContext.class.getDeclaredFields());
        var inningFields = Arrays.asList(inningContext.getDeclaredFields());
        var baseFields = Arrays.asList(AbstractBasesState.class.getDeclaredFields());
        boolean ownsInningContext = false;
        boolean ownsBaseState = false;
        for (var field : gameFields) {
            ownsInningContext |= field.getType() == inningContext;
            ownsBaseState |= BasesState.class.isAssignableFrom(field.getType());
        }
        var hasInningContext = ownsInningContext;
        var hasDirectBaseState = ownsBaseState;
        boolean ownsInningState = false;
        boolean hasGameReference = false;
        boolean hasInning = false;
        boolean hasScore = false;
        long concreteStateCount = 0;
        for (var field : inningFields) {
            ownsInningState |= field.getType() == InningState.class;
            hasGameReference |= field.getType() == GameBattingContext.class;
            hasInning |= field.getName().equals("inning") && field.getType() == long.class;
            hasScore |= field.getName().equals("score") && field.getType() == long.class;
            if (BasesState.class.isAssignableFrom(field.getType())
                    && field.getType() != BasesState.class) {
                concreteStateCount++;
            }
        }
        var hasInningState = ownsInningState;
        var ownedConcreteStates = concreteStateCount;
        var contextHasGameReference = hasGameReference;
        var contextHasInning = hasInning;
        var contextHasScore = hasScore;
        var processorParameters =
                AtBatProcessor.class
                        .getDeclaredMethod(
                                "process",
                                inningContext,
                                com.example.baseballorders.simulator.domain.player.BatterEntity
                                        .class)
                        .getParameterTypes();

        // then
        assertAll(
                () -> assertTrue(hasInningContext),
                () -> assertFalse(hasDirectBaseState),
                () -> assertTrue(hasInningState),
                () -> assertFalse(contextHasGameReference),
                () -> assertTrue(contextHasInning),
                () -> assertTrue(contextHasScore),
                () -> assertEquals(0, ownedConcreteStates),
                () ->
                        assertFalse(
                                baseFields.stream()
                                        .anyMatch(field -> field.getType() == InningState.class)),
                () -> assertEquals(inningContext, processorParameters[0]));
    }
}
