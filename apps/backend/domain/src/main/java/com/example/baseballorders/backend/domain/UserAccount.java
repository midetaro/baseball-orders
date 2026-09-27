package com.example.baseballorders.backend.domain;

import java.time.Instant;
import java.util.Objects;
import org.jilt.Builder;
import org.jilt.BuilderStyle;

/** ログインに必要なユーザーアカウント情報。 */
@Builder(style = BuilderStyle.STAGED)
public record UserAccount(
        Long id, String username, UserStatus status, Instant createdAt, Instant updatedAt) {
    /** 必須項目を検証してユーザーアカウントを作成する。 */
    public UserAccount {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
