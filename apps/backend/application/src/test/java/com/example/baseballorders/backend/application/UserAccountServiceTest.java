package com.example.baseballorders.backend.application;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.baseballorders.backend.domain.UserAccount;
import com.example.baseballorders.backend.domain.UserStatus;
import java.time.Instant;
import java.util.HashMap;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserAccountServiceTest {

    @Test
    @DisplayName("有効なユーザー名とパスワードでアカウントを登録するとパスワードをハッシュ化する")
    void registersAccountWithHashedPassword() {
        // given
        var repository = new InMemoryUserAccountRepository();
        var service = new UserAccountService(repository, new TestPasswordHasher());

        // when
        var account = service.register("baseball_user", "password-123");

        // then
        assertAll(
                () -> assertEquals("baseball_user", account.username()),
                () -> assertEquals(UserStatus.ACTIVE, account.status()),
                () ->
                        assertEquals(
                                true,
                                service.passwordMatches("password-123", account.passwordHash())));
    }

    @Test
    @DisplayName("重複したユーザー名は登録できない")
    void rejectsDuplicateUsername() {
        // given
        var repository = new InMemoryUserAccountRepository();
        var service = new UserAccountService(repository, new TestPasswordHasher());
        service.register("baseball_user", "password-123");

        // when
        var exception =
                assertThrows(
                        DuplicateUsernameException.class,
                        () -> service.register("baseball_user", "password-456"));

        // then
        assertAll(() -> assertEquals("username is already registered", exception.getMessage()));
    }

    @Test
    @DisplayName("LOCKED状態のユーザーは正しいパスワードでも認証できない")
    void rejectsLockedAccount() {
        // given
        var repository = new InMemoryUserAccountRepository();
        repository.add("locked_user", "hash:password-123", UserStatus.LOCKED);
        var service = new UserAccountService(repository, new TestPasswordHasher());

        // when
        var result = service.authenticate("locked_user", "password-123");

        // then
        assertAll(() -> assertEquals(false, result.isPresent()));
    }

    private static final class TestPasswordHasher implements PasswordHasher {
        @Override
        public String hash(String password) {
            return "hash:" + password;
        }

        @Override
        public boolean matches(String password, String passwordHash) {
            return passwordHash.equals(hash(password));
        }
    }

    private static final class InMemoryUserAccountRepository implements UserAccountRepository {
        private final HashMap<String, UserAccount> accounts = new HashMap<>();
        private long nextId = 1;

        @Override
        public Optional<UserAccount> findByUsername(String username) {
            return Optional.ofNullable(accounts.get(username));
        }

        @Override
        public UserAccount save(String username, String passwordHash, UserStatus status) {
            return add(username, passwordHash, status);
        }

        private UserAccount add(String username, String passwordHash, UserStatus status) {
            var now = Instant.now();
            var account = new UserAccount(nextId++, username, passwordHash, status, now, now);
            accounts.put(username, account);
            return account;
        }
    }
}
