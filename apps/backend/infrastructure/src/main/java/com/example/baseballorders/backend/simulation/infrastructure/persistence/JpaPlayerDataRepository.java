package com.example.baseballorders.backend.simulation.infrastructure.persistence;

import com.example.baseballorders.backend.simulation.application.PlayerDataRepository;
import com.example.baseballorders.backend.simulation.domain.PlayerData;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Objects;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** JPAを使用してplayersテーブルから選手の打撃データを取得するRepository。 */
@Repository
@RequiredArgsConstructor
public class JpaPlayerDataRepository implements PlayerDataRepository {

    @NonNull private final EntityManager entityManager;

    /** {@inheritDoc} */
    @Override
    public List<PlayerData> findAllByIds(List<Long> playerIds) {
        Objects.requireNonNull(playerIds, "playerIds must not be null");
        return playerIds.stream().map(this::findById).toList();
    }

    private PlayerData findById(Long playerId) {
        PlayerEntity player = entityManager.find(PlayerEntity.class, playerId);
        if (player == null) {
            throw new IllegalArgumentException("player not found: " + playerId);
        }
        return new PlayerData(
                player.getName(),
                player.getHitAverage(),
                player.getSluggish(),
                player.getBuntSuccessRate());
    }
}
