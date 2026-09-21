package com.example.baseballorders.backend.application;

import com.example.baseballorders.backend.domain.UserAccount;
import com.example.baseballorders.backend.domain.UserStatus;
import java.util.Optional;

/** ユーザーアカウントを永続化するポート。 */
public interface UserAccountRepository {

    /**
     * ユーザー名からアカウントを取得する。
     *
     * @param username ログイン用ユーザー名
     * @return 存在する場合のアカウント
     */
    Optional<UserAccount> findByUsername(String username);

    /**
     * 新しいアカウントを保存する。
     *
     * @param username ログイン用ユーザー名
     * @param passwordHash BCrypt等で生成したハッシュ
     * @param status 初期アカウント状態
     * @return 保存済みアカウント
     */
    UserAccount save(String username, String passwordHash, UserStatus status);
}
