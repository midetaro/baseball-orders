package com.example.baseballorders.backend.simulation.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.baseballorders.backend.simulation.infrastructure.persistence.PlayerEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimulationPageControllerTest {

    @Test
    @DisplayName("トップ画面を表示すると打者一覧をplayer ID順で渡す")
    void showsPlayersOnSimulationPage() {
        // given
        // EntityManagerとJPA queryをモックする
        EntityManager entityManager = mock(EntityManager.class);
        @SuppressWarnings("unchecked")
        TypedQuery<PlayerEntity> query = mock(TypedQuery.class);
        PlayerEntity first = mock(PlayerEntity.class);
        PlayerEntity second = mock(PlayerEntity.class);
        when(entityManager.createQuery(
                        "SELECT p FROM PlayerEntity p ORDER BY p.playerId", PlayerEntity.class))
                .thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(first, second));
        var controller = new SimulationPageController(entityManager);

        // when
        var page = controller.index();

        // then
        assertAll(
                () -> assertEquals("simulation", page.getViewName()),
                () -> assertEquals(List.of(first, second), page.getModel().get("players")));
    }
}
