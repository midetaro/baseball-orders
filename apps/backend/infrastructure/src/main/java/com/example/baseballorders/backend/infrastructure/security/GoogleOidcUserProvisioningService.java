package com.example.baseballorders.backend.infrastructure.security;

import com.example.baseballorders.backend.application.UserAccountService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/** Google OIDCの認証成功時にアカウントを作成または取得するユーザーサービス。 */
public class GoogleOidcUserProvisioningService
        implements OAuth2UserService<OidcUserRequest, OidcUser> {
    private final OAuth2UserService<OidcUserRequest, OidcUser> delegate;
    private final UserAccountService userAccountService;

    /**
     * ユーザー作成サービスを作成する。
     *
     * @param userAccountService Google subjectを永続化するユースケース
     */
    public GoogleOidcUserProvisioningService(UserAccountService userAccountService) {
        this(new OidcUserService(), userAccountService);
    }

    GoogleOidcUserProvisioningService(
            OAuth2UserService<OidcUserRequest, OidcUser> delegate,
            UserAccountService userAccountService) {
        this.delegate = delegate;
        this.userAccountService = userAccountService;
    }

    /**
     * GoogleからOIDCユーザーを読み込み、subjectに対応するアカウントを準備する。
     *
     * @param userRequest Googleへのユーザー情報要求
     * @return 認証済みOIDCユーザー
     */
    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        var oidcUser = delegate.loadUser(userRequest);
        userAccountService.provisionGoogleAccount(oidcUser.getSubject());
        return oidcUser;
    }
}
