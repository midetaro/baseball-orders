package com.example.baseballorders.backend.application;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.baseballorders.backend.domain.UserAccount;
import com.example.baseballorders.backend.domain.UserAccountBuilder;
import com.example.baseballorders.backend.domain.UserStatus;
import java.time.Instant;
import java.util.HashMap;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserAccountServiceTest {

    @Test
    @DisplayName("Google subjectで初回ログインするとアクティブなアカウントを作成する")
    void provisionsActiveAccountForGoogleSubject() {
        // given
        var repository = new InMemoryUserAccountRepository();
        var service = new UserAccountService(repository);

        // when
        var account = service.provisionGoogleAccount("google-subject-123");

        // then
        assertAll(
                () -> assertEquals("google:google-subject-123", account.username()),
                () -> assertEquals(UserStatus.ACTIVE, account.status()));
    }

    @Test
    @DisplayName("同じGoogle subjectで再ログインすると既存アカウントを返す")
    void returnsExistingAccountForSameGoogleSubject() {
        // given
        var repository = new InMemoryUserAccountRepository();
        var service = new UserAccountService(repository);
        var firstAccount = service.provisionGoogleAccount("google-subject-123");

        // when
        var account = service.provisionGoogleAccount("google-subject-123");

        // then
        assertAll(
                () -> assertEquals(firstAccount.id(), account.id()),
                () -> assertEquals(1, repository.accountCount()));
    }

    @Test
    @DisplayName("空白のGoogle subjectではアカウントを作成できない")
    void rejectsBlankGoogleSubject() {
        // given
        var repository = new InMemoryUserAccountRepository();
        var service = new UserAccountService(repository);

        // when
        var exception =
                assertThrows(
                        IllegalArgumentException.class, () -> service.provisionGoogleAccount(" "));

        // then
        assertAll(() -> assertEquals("googleSubject must not be blank", exception.getMessage()));
    }

    private static final class InMemoryUserAccountRepository implements UserAccountRepository {
        private final HashMap<String, UserAccount> accounts = new HashMap<>();
        private long nextId = 1;

        @Override
        public Optional<UserAccount> findByUsername(String username) {
            return Optional.ofNullable(accounts.get(username));
        }

        @Override
        public UserAccount save(String username, UserStatus status) {
            return add(username, status);
        }

        private UserAccount add(String username, UserStatus status) {
            var now = Instant.now();
            var account =
                    UserAccountBuilder.userAccount()
                            .id(nextId++)
                            .username(username)
                            .status(status)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
            accounts.put(username, account);
            return account;
        }

        private int accountCount() {
            return accounts.size();
        }
    }
}
