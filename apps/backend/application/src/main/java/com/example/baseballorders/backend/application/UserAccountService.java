package com.example.baseballorders.backend.application;

import com.example.baseballorders.backend.domain.UserAccount;
import java.util.Objects;

/** Google OIDCのユーザーアカウントを調整するユースケース。 */
public final class UserAccountService {
    private final UserAccountRepository repository;

    /**
     * アカウント管理ユースケースを作成する。
     *
     * @param repository アカウント永続化ポート
     */
    public UserAccountService(UserAccountRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    /**
     * Google OIDC subjectに対応するアカウントを取得し、初回だけ作成する。
     *
     * @param googleSubject Googleが発行する不変のOIDC subject
     * @return 既存または新規のアクティブアカウント
     * @throws IllegalArgumentException subjectが空白の場合
     */
    public UserAccount provisionGoogleAccount(String googleSubject) {
        if (googleSubject == null || googleSubject.isBlank()) {
            throw new IllegalArgumentException("googleSubject must not be blank");
        }
        var username = "google:" + googleSubject;
        return repository
                .findByUsername(username)
                .orElseGet(
                        () ->
                                repository.save(
                                        username,
                                        com.example.baseballorders.backend.domain.UserStatus
                                                .ACTIVE));
    }
}
