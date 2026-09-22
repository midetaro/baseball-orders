package com.example.baseballorders.backend.domain;

import java.util.Objects;
import org.jilt.Builder;
import org.jilt.BuilderStyle;

/** ローカルIDとパスワードで認証するための資格情報。 */
@Builder(style = BuilderStyle.STAGED)
public record LocalUserCredentials(String username, String passwordHash, UserStatus status) {
    /**
     * 必須項目を検証してローカル認証用資格情報を作成する。
     *
     * @param username ローカルアカウントを識別する内部ユーザー名
     * @param passwordHash BCryptでハッシュ化されたパスワード
     * @param status アカウント状態
     */
    public LocalUserCredentials {
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }
}
