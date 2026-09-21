package com.example.baseballorders.backend.infrastructure.persistence;

import com.example.baseballorders.backend.domain.UserStatus;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** usersテーブルへJPAでマッピングするユーザーアカウントEntity。 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class UserAccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * 保存するアカウントEntityを作成する。
     *
     * @param username ログイン用ユーザー名
     * @param passwordHash 保存用ハッシュ
     * @param status アカウント状態
     */
    public UserAccountEntity(String username, String passwordHash, UserStatus status) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.status = status;
    }
}
