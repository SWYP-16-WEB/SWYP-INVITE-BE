package com.swyp.invite.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.swyp.invite.global.error.ErrorCode;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = SecurityConfigTest.SampleController.class)
@Import({SecurityConfig.class, SecurityConfigTest.SampleController.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void 로그인하지_않고_회원_API를_부르면_401이다() throws Exception {
        mvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message").value(ErrorCode.UNAUTHENTICATED.getMessage()))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void 로그인하면_회원_API를_부를_수_있다() throws Exception {
        mvc.perform(get("/api/v1/users/me").with(user("1"))).andExpect(status().isOk());
    }

    @Test
    void 공개_API는_로그인_없이_부를_수_있다() throws Exception {
        mvc.perform(get("/api/v1/invitations/abc")).andExpect(status().isOk());
    }

    @Test
    void 공개_목록에_없는_경로는_로그인이_필요하다() throws Exception {
        mvc.perform(get("/api/v1/anything")).andExpect(status().isUnauthorized());
    }

    @Test
    void 이벤트_만들기는_로그인_없이_부를_수_있고_내_모임_목록은_회원만_본다() throws Exception {
        mvc.perform(post("/api/v1/events").with(csrfToken())).andExpect(status().isOk());
        mvc.perform(get("/api/v1/events")).andExpect(status().isUnauthorized());
    }

    @Test
    void 값을_바꾸는_요청에_CSRF_토큰이_없으면_403이다() throws Exception {
        mvc.perform(patch("/api/v1/users/me").with(user("1")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void CSRF_토큰_없이_부른_요청의_403_응답에도_토큰_쿠키가_실려_다시_시도할_수_있다() throws Exception {
        Cookie token = mvc.perform(post("/api/v1/events"))
                .andExpect(status().isForbidden())
                .andReturn()
                .getResponse()
                .getCookie("XSRF-TOKEN");

        mvc.perform(post("/api/v1/events").cookie(token).header("X-XSRF-TOKEN", token.getValue()))
                .andExpect(status().isOk());
    }

    @Test
    void 쿠키로_받은_CSRF_토큰을_헤더에_실으면_값을_바꿀_수_있다() throws Exception {
        Cookie token = mvc.perform(get("/api/v1/invitations/abc"))
                .andReturn()
                .getResponse()
                .getCookie("XSRF-TOKEN");

        assertThat(token).isNotNull();
        assertThat(token.isHttpOnly()).isFalse();
        mvc.perform(patch("/api/v1/users/me").with(user("1")).cookie(token).header("X-XSRF-TOKEN", token.getValue()))
                .andExpect(status().isOk());
    }

    @Test
    void 쿠키와_다른_값을_헤더에_실으면_403이다() throws Exception {
        Cookie token = mvc.perform(get("/api/v1/invitations/abc"))
                .andReturn()
                .getResponse()
                .getCookie("XSRF-TOKEN");

        mvc.perform(patch("/api/v1/users/me").with(user("1")).cookie(token).header("X-XSRF-TOKEN", "wrong"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
    }

    @Test
    void 쿠키로_회원을_확인하지_않는_공개_API는_CSRF_토큰_없이_값을_바꿀_수_있다() throws Exception {
        mvc.perform(post("/api/v1/invitations/abc/rsvps")).andExpect(status().isOk());
    }

    @Test
    void 로그아웃하면_204를_주고_세션을_지운다() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mvc.perform(post("/api/v1/auth/logout").session(session).with(user("1")).with(csrfToken()))
                .andExpect(status().isNoContent());

        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void 로그인하지_않고_로그아웃해도_204다() throws Exception {
        mvc.perform(post("/api/v1/auth/logout").with(csrfToken())).andExpect(status().isNoContent());
    }

    private RequestPostProcessor csrfToken() throws Exception {
        Cookie token = mvc.perform(get("/api/v1/invitations/abc"))
                .andReturn()
                .getResponse()
                .getCookie("XSRF-TOKEN");
        return request -> {
            request.setCookies(token);
            request.addHeader("X-XSRF-TOKEN", token.getValue());
            return request;
        };
    }

    @RestController
    static class SampleController {

        @GetMapping({"/api/v1/users/me", "/api/v1/invitations/abc"})
        void read() {}

        @PatchMapping("/api/v1/users/me")
        void change() {}

        @PostMapping({"/api/v1/events", "/api/v1/invitations/abc/rsvps"})
        void create() {}
    }
}
