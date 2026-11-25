package com.swyp.invite.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LoginPropertiesTest {

    @Test
    void 프론트엔드_주소로_카카오가_돌아올_주소와_성공_실패_화면_주소를_만든다() {
        LoginProperties properties = new LoginProperties(
                "https://front.example.com", "/login/callback", "/login/failed", kakao("id", "secret"));

        assertThat(properties.redirectUri()).isEqualTo("https://front.example.com/api/v1/auth/callback/kakao");
        assertThat(properties.successUrl()).isEqualTo("https://front.example.com/login/callback");
        assertThat(properties.failureUrl()).isEqualTo("https://front.example.com/login/failed?error=KAKAO_AUTH_FAILED");
    }

    @Test
    void 프론트엔드_주소가_비어_있으면_로컬_주소를_쓴다() {
        LoginProperties properties =
                new LoginProperties(" ", "/login/callback", "/login/failed", kakao("id", "secret"));

        assertThat(properties.frontendUrl()).isEqualTo("http://localhost:3000");
    }

    @Test
    void 카카오_키가_비어_있으면_설정_안_됨을_뜻하는_값이_된다() {
        LoginProperties.Kakao kakao = kakao("", null);

        assertThat(kakao.clientId()).isEqualTo("not-set");
        assertThat(kakao.clientSecret()).isEqualTo("not-set");
    }

    private LoginProperties.Kakao kakao(String clientId, String clientSecret) {
        return new LoginProperties.Kakao(
                clientId,
                clientSecret,
                "https://kauth.kakao.com/oauth/authorize",
                "https://kauth.kakao.com/oauth/token",
                "https://kapi.kakao.com/v2/user/me");
    }
}
