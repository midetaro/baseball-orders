package com.example.baseballorders.backend.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** UserAccountEntityのSpring Data JPAリポジトリ。 */
interface SpringDataUserAccountRepository extends JpaRepository<UserAccountEntity, Long> {

    /**
     * 一意ユーザー名でアカウントを取得する。
     *
     * @param username Google subject由来のユーザー名
     * @return 存在する場合のエンティティ
     */
    Optional<UserAccountEntity> findByUsername(String username);
}
