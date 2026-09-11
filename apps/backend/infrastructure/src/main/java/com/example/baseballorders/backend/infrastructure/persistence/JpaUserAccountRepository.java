package com.example.baseballorders.backend.infrastructure.persistence;

import com.example.baseballorders.backend.application.UserAccountRepository;
import com.example.baseballorders.backend.domain.UserAccount;
import com.example.baseballorders.backend.domain.UserStatus;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** JPAを使用してusersテーブルを操作するアカウントRepository。 */
@Repository
@RequiredArgsConstructor
public class JpaUserAccountRepository implements UserAccountRepository {
    private final EntityManager entityManager;

    /** {@inheritDoc} */
    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return entityManager
                .createQuery(
                        "SELECT u FROM UserAccountEntity u WHERE u.username = :username",
                        UserAccountEntity.class)
                .setParameter("username", username)
                .getResultList()
                .stream()
                .findFirst()
                .map(JpaUserAccountRepository::toDomain);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public UserAccount save(String username, String passwordHash, UserStatus status) {
        var entity = new UserAccountEntity(username, passwordHash, status);
        entityManager.persist(entity);
        entityManager.flush();
        return toDomain(entity);
    }

    private static UserAccount toDomain(UserAccountEntity entity) {
        return new UserAccount(
                entity.getId(),
                entity.getUsername(),
                entity.getPasswordHash(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
