package com.example.baseballorders.backend.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.baseballorders.backend.application.adapter.PlayerListQuery;
import com.example.baseballorders.backend.application.dto.PlayerListItem;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimulationPageControllerTest {

    @Test
    @DisplayName("トップ画面を表示すると打者一覧を渡す")
    void showsPlayersOnSimulationPage() {
        // given
        // 画面表示用の選手一覧を返すapplicationポートをモックする
        PlayerListQuery playerListQuery = mock(PlayerListQuery.class);
        var first = new PlayerListItem(1L, "山田", 0.301f, 0.501f, 0.701f, 0.801f);
        var second = new PlayerListItem(2L, "鈴木", 0.302f, 0.502f, 0.702f, 0.802f);
        when(playerListQuery.findAll()).thenReturn(List.of(first, second));
        var controller = new SimulationPageController(playerListQuery);

        // when
        var page = controller.index();

        // then
        assertAll(
                () -> assertEquals("simulation", page.getViewName()),
                () -> assertEquals(List.of(first, second), page.getModel().get("players")));
    }
}
