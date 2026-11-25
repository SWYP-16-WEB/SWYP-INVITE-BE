package com.swyp.invite.global.security;

import com.swyp.invite.global.error.ErrorCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.login")
public record LoginProperties(String frontendUrl, String successPath, String failurePath, Kakao kakao) {

    public LoginProperties {
        frontendUrl = orDefault(frontendUrl, "http://localhost:3000");
    }

    public String redirectUri() {
        return frontendUrl + "/api/v1/auth/callback/kakao";
    }

    public String successUrl() {
        return frontendUrl + successPath;
    }

    public String failureUrl() {
        return frontendUrl + failurePath + "?error=" + ErrorCode.KAKAO_AUTH_FAILED.name();
    }

    private static String orDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public record Kakao(
            String clientId, String clientSecret, String authorizationUri, String tokenUri, String userInfoUri) {

        public Kakao {
            clientId = orDefault(clientId, "not-set");
            clientSecret = orDefault(clientSecret, "not-set");
        }
    }
}
