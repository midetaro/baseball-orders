package com.example.baseballorders.backend.simulation.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JpaPlayerDataRepositoryTest {

    @Test
    @DisplayName("Player Entityの各項目をgetterで取得できる")
    void exposesPlayerPropertiesWithGetters() {
        // given
        var player = new PlayerEntity(1L, "山田", 0.301f, 0.501f, 0.701f);

        // when
        Long playerId = player.getPlayerId();
        String name = player.getName();
        float hitAverage = player.getHitAverage();
        float sluggish = player.getSluggish();
        float buntSuccessRate = player.getBuntSuccessRate();

        // then
        assertAll(
                () -> assertEquals(1L, playerId),
                () -> assertEquals("山田", name),
                () -> assertEquals(0.301f, hitAverage),
                () -> assertEquals(0.501f, sluggish),
                () -> assertEquals(0.701f, buntSuccessRate));
    }

    @Test
    @DisplayName("player IDを指定するとJPAから入力順に選手の打撃データを取得できる")
    void findsPlayerDataInRequestedOrder() {
        // given
        try (EntityManagerFactory factory = entityManagerFactory();
                EntityManager entityManager = factory.createEntityManager()) {
            persist(entityManager, new PlayerEntity(1L, "山田", 0.301f, 0.501f, 0.701f));
            persist(entityManager, new PlayerEntity(2L, "鈴木", 0.302f, 0.502f, 0.702f));
            var repository = new JpaPlayerDataRepository(entityManager);

            // when
            var players = repository.findAllByIds(List.of(2L, 1L));

            // then
            assertAll(
                    () ->
                            assertEquals(
                                    List.of("鈴木", "山田"),
                                    players.stream().map(player -> player.name()).toList()),
                    () -> assertEquals(0.302f, players.getFirst().hitAverage()),
                    () -> assertEquals(0.502f, players.getFirst().sluggish()),
                    () -> assertEquals(0.702f, players.getFirst().buntSuccessRate()));
        }
    }

    @Test
    @DisplayName("JPAに存在しないplayer IDを指定すると選手データを取得できない")
    void rejectsUnknownPlayerId() {
        // given
        try (EntityManagerFactory factory = entityManagerFactory();
                EntityManager entityManager = factory.createEntityManager()) {
            var repository = new JpaPlayerDataRepository(entityManager);

            // when
            var exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> repository.findAllByIds(List.of(999L)));

            // then
            assertAll(() -> assertEquals("player not found: 999", exception.getMessage()));
        }
    }

    private static EntityManagerFactory entityManagerFactory() {
        return Persistence.createEntityManagerFactory(
                "player-test",
                Map.of(
                        "jakarta.persistence.jdbc.url",
                        "jdbc:h2:mem:" + java.util.UUID.randomUUID(),
                        "jakarta.persistence.jdbc.user",
                        "sa",
                        "jakarta.persistence.jdbc.password",
                        "",
                        "hibernate.hbm2ddl.auto",
                        "create-drop"));
    }

    private static void persist(EntityManager entityManager, PlayerEntity player) {
        entityManager.getTransaction().begin();
        entityManager.persist(player);
        entityManager.getTransaction().commit();
        entityManager.clear();
    }
}
