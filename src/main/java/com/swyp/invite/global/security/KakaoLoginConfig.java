package com.swyp.invite.global.security;

import com.swyp.invite.user.service.UserRegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;

@Slf4j
@Configuration
@EnableConfigurationProperties(LoginProperties.class)
@RequiredArgsConstructor
public class KakaoLoginConfig {

    private static final String REGISTRATION_ID = "kakao";

    private static final int PRE_LOGIN_SESSION_SECONDS = 600;

    private final LoginProperties properties;

    private final UserRegistrationService userRegistrationService;

    @Bean
    ClientRegistrationRepository clientRegistrationRepository() {
        LoginProperties.Kakao kakao = properties.kakao();
        return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId(REGISTRATION_ID)
                .clientId(kakao.clientId())
                .clientSecret(kakao.clientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(properties.redirectUri())
                .scope("profile_nickname")
                .authorizationUri(kakao.authorizationUri())
                .tokenUri(kakao.tokenUri())
                .userInfoUri(kakao.userInfoUri())
                .userNameAttributeName("id")
                .build());
    }

    @Bean
    Customizer<OAuth2LoginConfigurer<HttpSecurity>> kakaoLogin(
            @Value("${server.servlet.session.timeout}") Duration sessionTimeout) {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> kakao = new DefaultOAuth2UserService();
        RedirectStrategy redirect = new DefaultRedirectStrategy();
        return oauth2 -> {
            oauth2.authorizationEndpoint(endpoint -> {
                endpoint.baseUri("/api/v1/auth/login");
                endpoint.authorizationRedirectStrategy((request, response, url) -> {
                    if (SecurityContextHolder.getContext().getAuthentication() == null) {
                        request.getSession().setMaxInactiveInterval(PRE_LOGIN_SESSION_SECONDS);
                    }
                    redirect.sendRedirect(request, response, url);
                });
            });
            oauth2.redirectionEndpoint(endpoint -> endpoint.baseUri("/api/v1/auth/callback/*"));
            oauth2.userInfoEndpoint(endpoint -> endpoint.userService(request -> loginUser(kakao.loadUser(request))));
            oauth2.authorizedClientRepository(new NoTokenRepository());
            oauth2.successHandler((request, response, authentication) -> {
                request.getSession().setMaxInactiveInterval((int) sessionTimeout.toSeconds());
                response.sendRedirect(properties.successUrl());
            });
            oauth2.failureHandler((request, response, exception) -> {
                log.warn("카카오 로그인 실패", exception);
                response.sendRedirect(properties.failureUrl());
            });
        };
    }

    private LoginUser loginUser(OAuth2User kakaoUser) {
        try {
            return new LoginUser(userRegistrationService.findOrRegister(kakaoUser.getName(), nickname(kakaoUser)));
        } catch (RuntimeException e) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("member_registration_failed"), e.getClass().getName());
        }
    }

    private String nickname(OAuth2User kakaoUser) {
        return kakaoUser.getAttribute("kakao_account") instanceof Map<?, ?> account
                        && account.get("profile") instanceof Map<?, ?> profile
                        && profile.get("nickname") instanceof String nickname
                ? nickname
                : null;
    }

    private static final class NoTokenRepository implements OAuth2AuthorizedClientRepository {

        @Override
        public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(
                String clientRegistrationId, Authentication principal, HttpServletRequest request) {
            return null;
        }

        @Override
        public void saveAuthorizedClient(
                OAuth2AuthorizedClient authorizedClient,
                Authentication principal,
                HttpServletRequest request,
                HttpServletResponse response) {}

        @Override
        public void removeAuthorizedClient(
                String clientRegistrationId,
                Authentication principal,
                HttpServletRequest request,
                HttpServletResponse response) {}
    }
}
