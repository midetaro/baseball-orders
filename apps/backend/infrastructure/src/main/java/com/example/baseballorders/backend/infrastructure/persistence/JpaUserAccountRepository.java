package com.example.baseballorders.backend.infrastructure.persistence;

import com.example.baseballorders.backend.application.UserAccountRepository;
import com.example.baseballorders.backend.domain.LocalUserCredentials;
import com.example.baseballorders.backend.domain.LocalUserCredentialsBuilder;
import com.example.baseballorders.backend.domain.UserAccount;
import com.example.baseballorders.backend.domain.UserAccountBuilder;
import com.example.baseballorders.backend.domain.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** application層のアカウント永続化ポートをJPAで実装するアダプター。 */
@Repository
public class JpaUserAccountRepository implements UserAccountRepository {
    private final SpringDataUserAccountRepository repository;
    private final Clock clock;

    /**
     * JPAアダプターを作成する。
     *
     * @param repository Spring Dataリポジトリ
     * @param clock 現在時刻の供給元
     */
    public JpaUserAccountRepository(SpringDataUserAccountRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** {@inheritDoc} */
    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return repository.findByUsername(username).map(this::toDomain);
    }

    /** {@inheritDoc} */
    @Override
    public UserAccount save(String username, UserStatus status) {
        var now = Instant.now(clock);
        var entity = new UserAccountEntity(username, null, status, now, now);
        return toDomain(repository.save(entity));
    }

    /** {@inheritDoc} */
    @Override
    public Optional<LocalUserCredentials> findLocalCredentials(String username) {
        return repository
                .findByUsername(username)
                .filter(entity -> entity.passwordHash() != null)
                .map(this::toLocalCredentials);
    }

    /** {@inheritDoc} */
    @Override
    public UserAccount saveLocal(String username, String passwordHash, UserStatus status) {
        var now = Instant.now(clock);
        var entity = new UserAccountEntity(username, passwordHash, status, now, now);
        return toDomain(repository.save(entity));
    }

    private UserAccount toDomain(UserAccountEntity entity) {
        return UserAccountBuilder.userAccount()
                .id(entity.id())
                .username(entity.username())
                .status(entity.status())
                .createdAt(entity.createdAt())
                .updatedAt(entity.updatedAt())
                .build();
    }

    private LocalUserCredentials toLocalCredentials(UserAccountEntity entity) {
        return LocalUserCredentialsBuilder.localUserCredentials()
                .username(entity.username())
                .passwordHash(entity.passwordHash())
                .status(entity.status())
                .build();
    }
}
