package com.swyp.invite.user.controller;

import static com.swyp.invite.global.security.LoginTestSupport.loginAs;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.swyp.invite.global.error.ApiException;
import com.swyp.invite.global.error.ErrorCode;
import com.swyp.invite.global.security.SecurityConfig;
import com.swyp.invite.user.dto.UserProfileResponse;
import com.swyp.invite.user.dto.UserProfileUpdateRequest;
import com.swyp.invite.user.service.UserService;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UserService userService;

    @Test
    void 조회는_로그인한_회원의_세_값을_주고_없는_값도_null로_싣는다() throws Exception {
        given(userService.getProfile(7L)).willReturn(new UserProfileResponse("모임장", null, null));

        mvc.perform(get("/api/v1/users/me").with(loginAs(7L)))
                .andExpect(status().isOk())
                .andExpect(content()
                        .json("{\"nickname\":\"모임장\",\"email\":null,\"settlementInfo\":null}", JsonCompareMode.STRICT));
    }

    @Test
    void 로그인하지_않은_조회와_수정은_401이다() throws Exception {
        mvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(update("{\"nickname\":\"모임장\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        verifyNoInteractions(userService);
    }

    @Test
    void 수정은_세션의_회원_id와_다듬은_값을_넘기고_바뀐_세_값을_준다() throws Exception {
        UserProfileUpdateRequest cleaned =
                new UserProfileUpdateRequest("새 이름", "user1@example.com", "가짜은행 000-000-000000");
        given(userService.updateProfile(7L, cleaned))
                .willReturn(new UserProfileResponse("새 이름", "user1@example.com", "가짜은행 000-000-000000"));

        mvc.perform(update("{\"nickname\":\"  새 이름 \",\"email\":\" user1@example.com\","
                                + "\"settlementInfo\":\"가짜은행 000-000-000000  \"}")
                        .with(loginAs(7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("새 이름"))
                .andExpect(jsonPath("$.email").value("user1@example.com"))
                .andExpect(jsonPath("$.settlementInfo").value("가짜은행 000-000-000000"));
        verify(userService).updateProfile(7L, cleaned);
    }

    @Test
    void 빈_값과_빠진_항목은_지움으로_넘어간다() throws Exception {
        mvc.perform(update("{\"nickname\":\"모임장\",\"email\":\"   \"}").with(loginAs(7L)))
                .andExpect(status().isOk());

        verify(userService).updateProfile(7L, new UserProfileUpdateRequest("모임장", null, null));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidBodies")
    void 규칙을_어기면_400이고_걸린_항목을_알려_준다(String rule, String field, String body) throws Exception {
        mvc.perform(update(body).with(loginAs(7L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value(field));
        verifyNoInteractions(userService);
    }

    static Stream<Arguments> invalidBodies() {
        return Stream.of(
                Arguments.of("이름 없음", "nickname", "{\"email\":\"user1@example.com\"}"),
                Arguments.of("이름이 공백뿐", "nickname", "{\"nickname\":\"   \"}"),
                Arguments.of("이름 31자", "nickname", "{\"nickname\":\"" + "가".repeat(31) + "\"}"),
                Arguments.of("이메일 형식 아님", "email", "{\"nickname\":\"모임장\",\"email\":\"not-an-email\"}"),
                Arguments.of(
                        "이메일 81자",
                        "email",
                        "{\"nickname\":\"모임장\",\"email\":\"" + "a".repeat(60) + "@" + "b".repeat(16) + ".com\"}"),
                Arguments.of(
                        "정산정보 41자",
                        "settlementInfo",
                        "{\"nickname\":\"모임장\",\"settlementInfo\":\"" + "가".repeat(41) + "\"}"));
    }

    @Test
    void 경계_값은_통과한다() throws Exception {
        String body = "{\"nickname\":\"" + "가".repeat(30) + "\",\"email\":\"" + "a".repeat(60) + "@" + "b".repeat(15)
                + ".com\",\"settlementInfo\":\"" + "가".repeat(40) + "\"}";

        mvc.perform(update(body).with(loginAs(7L))).andExpect(status().isOk());

        verify(userService).updateProfile(any(), any());
    }

    @Test
    void CSRF_토큰이_없는_수정은_403이다() throws Exception {
        mvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"모임장\"}")
                        .with(loginAs(7L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
        verifyNoInteractions(userService);
    }

    @Test
    void 세션은_있는데_회원이_없으면_401과_USER_NOT_FOUND를_준다() throws Exception {
        given(userService.getProfile(7L)).willThrow(new ApiException(ErrorCode.USER_NOT_FOUND));

        mvc.perform(get("/api/v1/users/me").with(loginAs(7L)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    private MockHttpServletRequestBuilder update(String body) {
        return put("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(csrf());
    }
}
