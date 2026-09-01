package com.example.baseballorders.backend.simulation.infrastructure.persistence;

import com.example.baseballorders.backend.simulation.infrastructure.messaging.PlayerData;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Repository;

/** JPAを使用してplayersテーブルから選手の打撃データを取得するRepository。 */
@Repository
public class JpaPlayerDataRepository implements PlayerDataRepository {

    private final EntityManager entityManager;

    /**
     * 選手Entityを検索するEntityManagerを指定してRepositoryを作成する。
     *
     * @param entityManager JPAのEntityManager
     * @throws NullPointerException entityManagerがnullの場合
     */
    public JpaPlayerDataRepository(EntityManager entityManager) {
        this.entityManager =
                Objects.requireNonNull(entityManager, "entityManager must not be null");
    }

    /** {@inheritDoc} */
    @Override
    public List<PlayerData> findAllByIds(List<String> playerIds) {
        Objects.requireNonNull(playerIds, "playerIds must not be null");
        return playerIds.stream().map(this::findById).toList();
    }

    private PlayerData findById(String playerId) {
        PlayerEntity player = entityManager.find(PlayerEntity.class, playerId);
        if (player == null) {
            throw new IllegalArgumentException("player not found: " + playerId);
        }
        return new PlayerData(player.getName(), player.getHitAverage(), player.getSluggish());
    }
}
