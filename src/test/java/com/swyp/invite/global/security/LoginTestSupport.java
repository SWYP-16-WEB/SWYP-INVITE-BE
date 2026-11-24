package com.swyp.invite.global.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class LoginTestSupport {

    private LoginTestSupport() {}

    public static RequestPostProcessor loginAs(long userId) {
        return authentication(
                UsernamePasswordAuthenticationToken.authenticated(new LoginUser(userId), null, List.of()));
    }
}
