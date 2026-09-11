package com.example.baseballorders.backend.infrastructure.persistence;

import com.example.baseballorders.backend.application.SimulationStatisticsRepository;
import com.example.baseballorders.backend.domain.SimulationStatistics;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** JPAを使用してユーザー別統計情報を操作するRepository。 */
@Repository
@RequiredArgsConstructor
public class JpaSimulationStatisticsRepository implements SimulationStatisticsRepository {
    private final EntityManager entityManager;

    /** {@inheritDoc} */
    @Override
    @Transactional
    public SimulationStatistics save(SimulationStatistics statistics) {
        entityManager.persist(
                new SimulationStatisticsEntity(
                        statistics.simulationId(),
                        statistics.userId(),
                        statistics.averageScore(),
                        statistics.medianScore(),
                        statistics.maximumScore(),
                        statistics.createdAt()));
        return statistics;
    }

    /** {@inheritDoc} */
    @Override
    public List<SimulationStatistics> findAllByUserId(Long userId) {
        return entityManager
                .createQuery(
                        "SELECT s FROM SimulationStatisticsEntity s WHERE s.userId = :userId ORDER BY s.createdAt",
                        SimulationStatisticsEntity.class)
                .setParameter("userId", userId)
                .getResultList()
                .stream()
                .map(JpaSimulationStatisticsRepository::toDomain)
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<SimulationStatistics> findBySimulationIdAndUserId(
            UUID simulationId, Long userId) {
        return entityManager
                .createQuery(
                        "SELECT s FROM SimulationStatisticsEntity s WHERE s.simulationId = :simulationId AND s.userId = :userId",
                        SimulationStatisticsEntity.class)
                .setParameter("simulationId", simulationId)
                .setParameter("userId", userId)
                .getResultStream()
                .findFirst()
                .map(JpaSimulationStatisticsRepository::toDomain);
    }

    private static SimulationStatistics toDomain(SimulationStatisticsEntity entity) {
        return new SimulationStatistics(
                entity.getSimulationId(),
                entity.getUserId(),
                entity.getAverageScore(),
                entity.getMedianScore(),
                entity.getMaximumScore(),
                entity.getCreatedAt());
    }
}
