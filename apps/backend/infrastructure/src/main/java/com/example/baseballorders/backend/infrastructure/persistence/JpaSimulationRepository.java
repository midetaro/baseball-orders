package com.example.baseballorders.backend.infrastructure.persistence;

import com.example.baseballorders.backend.application.SimulationRepository;
import com.example.baseballorders.backend.domain.Simulation;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** JPAを使用してシミュレーションを操作するRepository。 */
@Repository
@RequiredArgsConstructor
public class JpaSimulationRepository implements SimulationRepository {
    private final EntityManager entityManager;

    /** {@inheritDoc} */
    @Override
    @Transactional
    public Simulation save(Simulation simulation) {
        var entity = entityManager.find(SimulationEntity.class, simulation.simulationId());
        if (entity == null) {
            entity =
                    new SimulationEntity(
                            simulation.simulationId(),
                            simulation.userId(),
                            simulation.status(),
                            simulation.createdAt(),
                            simulation.completedAt());
            entityManager.persist(entity);
        } else {
            entityManager.merge(
                    new SimulationEntity(
                            simulation.simulationId(),
                            simulation.userId(),
                            simulation.status(),
                            simulation.createdAt(),
                            simulation.completedAt()));
        }
        return simulation;
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Simulation> findBySimulationId(UUID simulationId) {
        return Optional.ofNullable(entityManager.find(SimulationEntity.class, simulationId))
                .map(JpaSimulationRepository::toDomain);
    }

    private static Simulation toDomain(SimulationEntity entity) {
        return new Simulation(
                entity.getSimulationId(),
                entity.getUserId(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getCompletedAt());
    }
}
