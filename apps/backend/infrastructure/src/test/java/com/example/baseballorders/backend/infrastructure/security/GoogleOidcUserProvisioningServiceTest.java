package com.example.baseballorders.backend.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.baseballorders.backend.application.UserAccountRepository;
import com.example.baseballorders.backend.application.UserAccountService;
import com.example.baseballorders.backend.domain.UserAccount;
import com.example.baseballorders.backend.domain.UserAccountBuilder;
import com.example.baseballorders.backend.domain.UserStatus;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

class GoogleOidcUserProvisioningServiceTest {

    @Test
    @DisplayName("Google OIDCの認証成功時にsubject由来のアカウントを作成する")
    void provisionsAccountOnSuccessfulOidcLogin() {
        // given
        var repository = new InMemoryUserAccountRepository();
        var oidcUserService = mock(OAuth2UserService.class);
        var request = mock(OidcUserRequest.class);
        var oidcUser = mock(OidcUser.class);
        when(oidcUserService.loadUser(request)).thenReturn(oidcUser);
        when(oidcUser.getSubject()).thenReturn("google-subject-123");
        var service =
                new GoogleOidcUserProvisioningService(
                        oidcUserService, new UserAccountService(repository));

        // when
        var authenticatedUser = service.loadUser(request);

        // then
        assertAll(
                () -> assertSame(oidcUser, authenticatedUser),
                () -> assertEquals("google:google-subject-123", repository.account().username()),
                () -> assertEquals(UserStatus.ACTIVE, repository.account().status()));
    }

    private static final class InMemoryUserAccountRepository implements UserAccountRepository {
        private UserAccount account;

        @Override
        public Optional<UserAccount> findByUsername(String username) {
            return Optional.ofNullable(account).filter(value -> value.username().equals(username));
        }

        @Override
        public UserAccount save(String username, UserStatus status) {
            var now = Instant.EPOCH;
            account =
                    UserAccountBuilder.userAccount()
                            .id(1L)
                            .username(username)
                            .status(status)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
            return account;
        }

        private UserAccount account() {
            return account;
        }
    }
}
