package com.example.baseballorders.backend.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimulationPageControllerTest {

    @Test
    @DisplayName("トップ画面を表示すると空の入力画面を返す")
    void showsEmptySimulationPage() {
        // given
        var controller = new SimulationPageController();

        // when
        var page = controller.index();

        // then
        assertAll(
                () -> assertEquals("simulation", page.getViewName()),
                () -> assertEquals(true, page.getModel().isEmpty()));
    }
}
