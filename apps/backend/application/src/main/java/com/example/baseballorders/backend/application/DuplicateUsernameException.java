package com.example.baseballorders.backend.application;

/** 既に使用されているユーザー名で登録しようとしたことを表す。 */
public final class DuplicateUsernameException extends RuntimeException {
    /** 重複ユーザー名例外を作成する。 */
    public DuplicateUsernameException() {
        super("username is already registered");
    }
}
