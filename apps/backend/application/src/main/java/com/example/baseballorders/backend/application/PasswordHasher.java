package com.example.baseballorders.backend.application;

/** パスワードのハッシュ化と照合を行うポート。 */
public interface PasswordHasher {

    /**
     * パスワードを不可逆ハッシュへ変換する。
     *
     * @param password 平文パスワード
     * @return 保存可能なハッシュ
     */
    String hash(String password);

    /**
     * 平文パスワードと保存済みハッシュを照合する。
     *
     * @param password 平文パスワード
     * @param passwordHash 保存済みハッシュ
     * @return 一致する場合true
     */
    boolean matches(String password, String passwordHash);
}
