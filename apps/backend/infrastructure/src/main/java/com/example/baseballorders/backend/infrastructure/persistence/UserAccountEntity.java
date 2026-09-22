package com.example.baseballorders.backend.infrastructure.persistence;

import com.example.baseballorders.backend.domain.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** usersテーブルに保存するGoogle OIDCおよびローカルアカウントの永続化エンティティ。 */
@Entity
@Table(name = "users")
public class UserAccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    /** JPAがエンティティを復元するためのコンストラクタ。 */
    protected UserAccountEntity() {}

    /**
     * 永続化するアカウントを作成する。
     *
     * @param username 認証方式から導出した一意のユーザー名
     * @param passwordHash ローカルアカウント用のBCryptパスワードハッシュ。Googleアカウントではnull
     * @param status アカウント状態
     * @param createdAt 作成日時
     * @param updatedAt 更新日時
     */
    public UserAccountEntity(
            String username,
            String passwordHash,
            UserStatus status,
            Instant createdAt,
            Instant updatedAt) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * エンティティIDを返す。
     *
     * @return 自動採番されたID
     */
    public Long id() {
        return id;
    }

    /**
     * 一意ユーザー名を返す。
     *
     * @return Google subject由来のユーザー名
     */
    public String username() {
        return username;
    }

    /**
     * ローカルアカウントのパスワードハッシュを返す。
     *
     * @return BCryptパスワードハッシュ。Googleアカウントではnull
     */
    public String passwordHash() {
        return passwordHash;
    }

    /**
     * アカウント状態を返す。
     *
     * @return 現在のアカウント状態
     */
    public UserStatus status() {
        return status;
    }

    /**
     * 作成日時を返す。
     *
     * @return 作成日時
     */
    public Instant createdAt() {
        return createdAt;
    }

    /**
     * 更新日時を返す。
     *
     * @return 更新日時
     */
    public Instant updatedAt() {
        return updatedAt;
    }
}
