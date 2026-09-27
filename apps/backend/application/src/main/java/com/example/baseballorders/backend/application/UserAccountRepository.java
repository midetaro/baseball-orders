package com.example.baseballorders.backend.application;

import com.example.baseballorders.backend.domain.LocalUserCredentials;
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

    /**
     * ローカルログインIDに対応する認証情報を取得する。
     *
     * @param username ローカルIDから導出した一意の内部ユーザー名
     * @return 存在する場合の認証情報
     */
    Optional<LocalUserCredentials> findLocalCredentials(String username);

    /**
     * パスワードハッシュを持つローカルアカウントを保存する。
     *
     * @param username ローカルIDから導出した一意の内部ユーザー名
     * @param passwordHash BCryptでハッシュ化したパスワード
     * @param status 初期アカウント状態
     * @return 保存済みアカウント
     */
    UserAccount saveLocal(String username, String passwordHash, UserStatus status);
}
