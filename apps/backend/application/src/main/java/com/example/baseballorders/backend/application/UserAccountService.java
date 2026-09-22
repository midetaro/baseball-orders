package com.example.baseballorders.backend.application;

import com.example.baseballorders.backend.domain.LocalUserCredentials;
import com.example.baseballorders.backend.domain.UserAccount;
import com.example.baseballorders.backend.domain.UserStatus;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/** Google OIDCのユーザーアカウントを調整するユースケース。 */
public final class UserAccountService {
    private static final String LOCAL_USERNAME_PREFIX = "local:";
    private static final Pattern LOCAL_ID_PATTERN = Pattern.compile("[A-Za-z0-9._-]{3,50}");
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
                .orElseGet(() -> repository.save(username, UserStatus.ACTIVE));
    }

    /**
     * ユーザー定義IDとパスワードハッシュでローカルアカウントを登録する。
     *
     * @param userId 3〜50文字の英数字、ドット、アンダースコア、ハイフンから成るID
     * @param passwordHash BCryptでハッシュ化した空でないパスワード
     * @return 新規アクティブアカウント
     * @throws IllegalArgumentException IDまたはパスワードハッシュが不正、またはIDが重複する場合
     */
    public UserAccount registerLocalAccount(String userId, String passwordHash) {
        var username = localUsername(userId);
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash must not be blank");
        }
        if (repository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("local account already exists");
        }
        return repository.saveLocal(username, passwordHash, UserStatus.ACTIVE);
    }

    /**
     * ユーザー定義IDのローカル認証情報を取得する。
     *
     * @param userId ログイン画面で入力されたID
     * @return 存在する場合の認証情報
     * @throws IllegalArgumentException IDの形式が不正な場合
     */
    public Optional<LocalUserCredentials> findLocalCredentials(String userId) {
        return repository.findLocalCredentials(localUsername(userId));
    }

    private String localUsername(String userId) {
        if (userId == null || !LOCAL_ID_PATTERN.matcher(userId).matches()) {
            throw new IllegalArgumentException("userId must be 3 to 50 URL-safe characters");
        }
        return LOCAL_USERNAME_PREFIX + userId;
    }
}
