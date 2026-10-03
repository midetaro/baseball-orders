package com.example.baseballorders.messaging;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimulationPlayerMessageTest {

    @Test
    @DisplayName("バント実行有無がnullの選手メッセージを拒否する")
    void rejectsNullBuntEnabled() {
        // given

        // when
        var exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new SimulationPlayerMessage("選手1", 0.3f, null, true));

        // then
        assertAll(() -> assertEquals("buntEnabled must not be null", exception.getMessage()));
    }

    @Test
    @DisplayName("選手メッセージは長打率を項目に持たない")
    void doesNotCarrySluggish() {
        // given

        // when
        var componentNames =
                Arrays.stream(SimulationPlayerMessage.class.getRecordComponents())
                        .map(RecordComponent::getName)
                        .toList();

        // then
        assertAll(() -> assertFalse(componentNames.contains("sluggish")));
    }
}
