package com.example.baseballorders.messaging;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
                        () -> new SimulationPlayerMessage("選手1", 0.3f, 0.4f, 0.7f, null, 0.8f));

        // then
        assertAll(() -> assertEquals("buntEnabled must not be null", exception.getMessage()));
    }
}
