package com.example.baseballorders.backend.infrastructure.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** login_user_idだけを保持するHTTPセッションをSpring Securityの認証へ変換する。 */
public final class SessionAuthenticationFilter extends OncePerRequestFilter {
    /** セッションへ保存するログイン済みユーザーIDの属性名。 */
    public static final String LOGIN_USER_ID = "login_user_id";

    /** {@inheritDoc} */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var session = request.getSession(false);
        if (session != null && session.getAttribute(LOGIN_USER_ID) instanceof Long userId)
            SecurityContextHolder.getContext()
                    .setAuthentication(
                            new UsernamePasswordAuthenticationToken(
                                    userId,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        filterChain.doFilter(request, response);
    }
}
