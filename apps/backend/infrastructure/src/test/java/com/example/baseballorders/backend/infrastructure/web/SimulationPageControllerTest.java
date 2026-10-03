package com.example.baseballorders.backend.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimulationPageControllerTest {

    @Test
    @DisplayName("1試合実行画面を通常・安打・得点・本塁打・バント成功のフレーム表示時間とともに返す")
    void showsSingleGamePage() {
        // given
        var sut = new SimulationPageController(1000L, 1800L, 2200L, 3600L, 1600L);

        // when
        var page = sut.singleGame();

        // then
        assertAll(
                () -> assertEquals("single-game", page.getViewName()),
                () -> assertEquals(1000L, page.getModel().get("frameDurationMillis")),
                () -> assertEquals(1800L, page.getModel().get("hitFrameDurationMillis")),
                () -> assertEquals(2200L, page.getModel().get("scoreFrameDurationMillis")),
                () -> assertEquals(3600L, page.getModel().get("homeRunFrameDurationMillis")),
                () -> assertEquals(1600L, page.getModel().get("buntFrameDurationMillis")));
    }

    @Test
    @DisplayName("トップ画面は打順組み替え画面を返す")
    void showsBattingOrderAtRoot() {
        // given
        var sut = new SimulationPageController(1000L, 1800L, 2200L, 3600L, 1600L);
        // when
        var page = sut.index();
        // then
        assertAll(
                () -> assertEquals("batting-order", page.getViewName()),
                () -> assertEquals(true, page.getModel().isEmpty()));
    }

    @Test
    @DisplayName("大規模実行画面を表示すると既存の入力画面を返す")
    void showsLargeScaleSimulationPage() {
        // given
        var controller = new SimulationPageController(1000L, 1800L, 2200L, 3600L, 1600L);

        // when
        var page = controller.largeScale();

        // then
        assertAll(
                () -> assertEquals("simulation", page.getViewName()),
                () -> assertEquals(true, page.getModel().isEmpty()));
    }
}
