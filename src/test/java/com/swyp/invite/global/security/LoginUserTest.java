package com.swyp.invite.global.security;

import static com.swyp.invite.global.security.LoginTestSupport.loginAs;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = LoginUserTest.SampleController.class)
@Import({SecurityConfig.class, LoginUserTest.SampleController.class})
class LoginUserTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void 회원_API의_컨트롤러는_로그인한_회원의_id를_받는다() throws Exception {
        mvc.perform(get("/api/v1/users/me").with(loginAs(7L)))
                .andExpect(status().isOk())
                .andExpect(content().string("7"));
    }

    @Test
    void 로그인_없이도_되는_API는_로그인한_요청이면_회원의_id를_받는다() throws Exception {
        mvc.perform(post("/api/v1/events").with(loginAs(7L)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("회원 7"));
    }

    @Test
    void 로그인_없이도_되는_API는_로그인하지_않은_요청이면_LoginUser가_비어_있다() throws Exception {
        mvc.perform(post("/api/v1/events").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("비회원"));
    }

    @RestController
    static class SampleController {

        @GetMapping("/api/v1/users/me")
        Long member(@AuthenticationPrincipal LoginUser loginUser) {
            return loginUser.userId();
        }

        @PostMapping("/api/v1/events")
        String memberOrNot(@AuthenticationPrincipal LoginUser loginUser) {
            return loginUser == null ? "비회원" : "회원 " + loginUser.userId();
        }
    }
}
