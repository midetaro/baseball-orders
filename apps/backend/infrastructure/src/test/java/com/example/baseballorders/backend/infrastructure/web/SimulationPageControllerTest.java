package com.example.baseballorders.backend.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimulationPageControllerTest {

    @Test
    @DisplayName("1試合実行画面をアニメーション用フレーム間隔とともに返す")
    void showsSingleGamePage() {
        // given
        var sut = new SimulationPageController(1000L);

        // when
        var page = sut.singleGame();

        // then
        assertAll(
                () -> assertEquals("single-game", page.getViewName()),
                () -> assertEquals(1000L, page.getModel().get("frameDurationMillis")));
    }

    @Test
    @DisplayName("トップ画面は大規模実行画面を返す")
    void showsLargeScaleAtRoot() {
        // given
        var sut = new SimulationPageController(1000L);
        // when
        var page = sut.index();
        // then
        assertAll(() -> assertEquals("simulation", page.getViewName()));
    }

    @Test
    @DisplayName("大規模実行画面を表示すると既存の入力画面を返す")
    void showsLargeScaleSimulationPage() {
        // given
        var controller = new SimulationPageController(1000L);

        // when
        var page = controller.largeScale();

        // then
        assertAll(
                () -> assertEquals("simulation", page.getViewName()),
                () -> assertEquals(true, page.getModel().isEmpty()));
    }
}
