package com.example.baseballorders.backend.application;

import com.example.baseballorders.backend.domain.UserAccount;
import com.example.baseballorders.backend.domain.UserStatus;
import java.util.Objects;

/** ユーザーアカウントの登録と認証を調整するユースケース。 */
public final class UserAccountService {
    private final UserAccountRepository repository;
    private final PasswordHasher passwordHasher;

    /**
     * アカウント管理ユースケースを作成する。
     *
     * @param repository アカウント永続化ポート
     * @param passwordHasher パスワードハッシュ化ポート
     */
    public UserAccountService(UserAccountRepository repository, PasswordHasher passwordHasher) {
        this.repository = Objects.requireNonNull(repository);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
    }

    /**
     * 入力値を検証してアクティブなアカウントを登録する。
     *
     * @param username ログイン用ユーザー名
     * @param password 平文パスワード
     * @return 保存済みアカウント
     * @throws IllegalArgumentException 入力値がポリシーに適合しない場合
     * @throws DuplicateUsernameException ユーザー名が既に登録されている場合
     */
    public UserAccount register(String username, String password) {
        validate(username, password);
        if (repository.findByUsername(username).isPresent()) {
            throw new DuplicateUsernameException();
        }
        return repository.save(username, passwordHasher.hash(password), UserStatus.ACTIVE);
    }

    /**
     * 認証情報が有効なアクティブアカウントに一致するかを判定する。
     *
     * @param username ログイン用ユーザー名
     * @param password 平文パスワード
     * @return 認証済みアカウント。失敗時は空
     */
    public java.util.Optional<UserAccount> authenticate(String username, String password) {
        return repository
                .findByUsername(username)
                .filter(account -> account.status() == UserStatus.ACTIVE)
                .filter(account -> passwordHasher.matches(password, account.passwordHash()));
    }

    /**
     * テスト用途を含め、保存済みハッシュと平文を照合する。
     *
     * @param password 平文パスワード
     * @param passwordHash 保存済みハッシュ
     * @return 一致する場合true
     */
    public boolean passwordMatches(String password, String passwordHash) {
        return passwordHasher.matches(password, passwordHash);
    }

    private static void validate(String username, String password) {
        if (username == null || !username.matches("[A-Za-z0-9_-]{3,50}")) {
            throw new IllegalArgumentException(
                    "username must be 3-50 alphanumeric, underscore, or hyphen characters");
        }
        if (password == null || password.length() < 8 || password.length() > 72) {
            throw new IllegalArgumentException("password must be 8-72 characters");
        }
    }
}
