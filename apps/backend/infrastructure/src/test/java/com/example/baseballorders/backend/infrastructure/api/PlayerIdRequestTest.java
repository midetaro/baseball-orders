package com.example.baseballorders.backend.infrastructure.api;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PlayerIdRequestTest {

    @Test
    @DisplayName("バント実行有無をAPI入力として保持する")
    void exposesBuntEnabled() {
        // given
        var components = PlayerIdRequest.class.getRecordComponents();

        // when
        var componentNames =
                Arrays.stream(components).map(component -> component.getName()).toList();

        // then
        assertAll(() -> assertEquals(List.of("playerId", "buntEnabled"), componentNames));
    }

    @Test
    @DisplayName("バント実行有無がnullのAPI入力を拒否する")
    void rejectsNullBuntEnabled() {
        // given

        // when
        var exception =
                assertThrows(NullPointerException.class, () -> new PlayerIdRequest(1L, null));

        // then
        assertAll(() -> assertEquals("bunt_enabled must not be null", exception.getMessage()));
    }
}
