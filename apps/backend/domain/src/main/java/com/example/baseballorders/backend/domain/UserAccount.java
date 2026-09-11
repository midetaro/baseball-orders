package com.example.baseballorders.backend.domain;

import java.time.Instant;
import java.util.Objects;

/** ログインに必要なユーザーアカウント情報。 */
public record UserAccount(
        Long id,
        String username,
        String passwordHash,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt) {
    /** 必須項目を検証してユーザーアカウントを作成する。 */
    public UserAccount {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
