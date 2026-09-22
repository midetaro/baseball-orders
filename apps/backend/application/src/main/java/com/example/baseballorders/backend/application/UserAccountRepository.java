package com.example.baseballorders.backend.application;

import com.example.baseballorders.backend.domain.UserAccount;
import com.example.baseballorders.backend.domain.UserStatus;
import java.util.Optional;

/** ユーザーアカウントを永続化するポート。 */
public interface UserAccountRepository {

    /**
     * ユーザー名からアカウントを取得する。
     *
     * @param username Google subjectから導出した一意のユーザー名
     * @return 存在する場合のアカウント
     */
    Optional<UserAccount> findByUsername(String username);

    /**
     * 新しいアカウントを保存する。
     *
     * @param username Google subjectから導出した一意のユーザー名
     * @param status 初期アカウント状態
     * @return 保存済みアカウント
     */
    UserAccount save(String username, UserStatus status);
}
