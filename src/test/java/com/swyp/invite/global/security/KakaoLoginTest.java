package com.swyp.invite.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.swyp.invite.TestcontainersConfiguration;
import com.swyp.invite.user.service.UserRegistrationService;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@Import({TestcontainersConfiguration.class, KakaoLoginTest.ProbeController.class})
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class KakaoLoginTest {

    private static final int TEN_MINUTES = 10 * 60;
    private static final int FOURTEEN_DAYS = 14 * 24 * 60 * 60;
    private static final String SUCCESS_URL = "https://front.example.com/login/callback";
    private static final String FAILURE_URL = "https://front.example.com/login/failed?error=KAKAO_AUTH_FAILED";

    private static final String TOKEN_JSON =
            "{\"access_token\":\"fake-access-token\",\"token_type\":\"bearer\",\"expires_in\":43199}";
    private static final String TOKEN_ERROR_JSON =
            "{\"error\":\"invalid_client\",\"error_description\":\"Bad client credentials\"}";

    private static volatile int tokenStatus;
    private static volatile String userJson;
    private static volatile String tokenRequestBody;
    private static final HttpServer KAKAO = startFakeKakao();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private EntityManager em;

    @Autowired
    private ObjectProvider<OAuth2AuthorizedClientService> authorizedClients;

    @MockitoSpyBean
    private UserRegistrationService registration;

    @DynamicPropertySource
    static void kakao(DynamicPropertyRegistry registry) {
        String kakao = "http://localhost:" + KAKAO.getAddress().getPort();
        registry.add("app.login.kakao.token-uri", () -> kakao + "/oauth/token");
        registry.add("app.login.kakao.user-info-uri", () -> kakao + "/v2/user/me");
        registry.add("app.login.kakao.client-id", () -> "test-client-id");
        registry.add("app.login.kakao.client-secret", () -> "test-client-secret");
        registry.add("app.login.frontend-url", () -> "https://front.example.com");
    }

    @BeforeEach
    void resetFakeKakao() {
        tokenStatus = 200;
        userJson = user(1001, "모임장");
        tokenRequestBody = null;
    }

    @Test
    void 로그인_시작은_카카오_인가_주소로_보내고_PKCE_값을_싣는다() throws Exception {
        String url = startLogin(new MockHttpSession());

        assertThat(url).startsWith("https://kauth.kakao.com/oauth/authorize?");
        MultiValueMap<String, String> query =
                UriComponentsBuilder.fromUriString(url).build().getQueryParams();
        assertThat(decode(query.getFirst("client_id"))).isEqualTo("test-client-id");
        assertThat(decode(query.getFirst("redirect_uri")))
                .isEqualTo("https://front.example.com/api/v1/auth/callback/kakao");
        assertThat(decode(query.getFirst("scope"))).isEqualTo("profile_nickname");
        assertThat(query.getFirst("response_type")).isEqualTo("code");
        assertThat(query.getFirst("state")).isNotBlank();
        assertThat(query.getFirst("code_challenge")).isNotBlank();
        assertThat(query.getFirst("code_challenge_method")).isEqualTo("S256");
    }

    @Test
    void 처음_로그인하면_회원이_만들어지고_성공_화면으로_간다() throws Exception {
        login(new MockHttpSession()).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(SUCCESS_URL));

        assertThat(em.createNativeQuery("select nickname from users where kakao_id = '1001'")
                        .getSingleResult())
                .isEqualTo("모임장");
    }

    @Test
    void 토큰_요청에_비밀_키와_돌아올_주소를_싣는다() throws Exception {
        login(new MockHttpSession());

        assertThat(decode(tokenRequestBody))
                .contains("grant_type=authorization_code", "code=code-1", "client_id=test-client-id")
                .contains("client_secret=test-client-secret")
                .contains("redirect_uri=https://front.example.com/api/v1/auth/callback/kakao");
    }

    @Test
    void 토큰_요청의_PKCE_값은_인가_요청에_실은_값과_짝이_맞는다() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String url = startLogin(session);
        String challenge =
                UriComponentsBuilder.fromUriString(url).build().getQueryParams().getFirst("code_challenge");

        mvc.perform(get("/api/v1/auth/callback/kakao")
                .param("code", "code-1")
                .param("state", stateOf(url))
                .session(session));

        String verifier = formValue(tokenRequestBody, "code_verifier");
        assertThat(verifier).isNotBlank();
        assertThat(sha256(verifier)).isEqualTo(challenge);
    }

    @Test
    void 로그인한_뒤_같은_세션으로_회원_API를_부르면_그_회원의_LoginUser가_들어온다() throws Exception {
        MockHttpSession session = new MockHttpSession();
        login(session);
        Object userId = em.createNativeQuery("select id from users where kakao_id = '1001'")
                .getSingleResult();

        mvc.perform(get("/api/v1/kakao-login-test/me").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(String.valueOf(userId)));
    }

    @Test
    void 다시_로그인하면_회원이_늘지_않고_이름이_바뀌지_않는다() throws Exception {
        login(new MockHttpSession());
        userJson = user(1001, "바뀐 닉네임");

        login(new MockHttpSession()).andExpect(redirectedUrl(SUCCESS_URL));

        assertThat(em.createNativeQuery("select nickname from users where kakao_id = '1001'")
                        .getResultList())
                .containsExactly("모임장");
    }

    @Test
    void 닉네임이_없으면_실패_화면으로_가고_회원이_만들어지지_않는다() throws Exception {
        userJson = "{\"id\":1001,\"kakao_account\":{\"profile\":{}}}";
        MockHttpSession session = new MockHttpSession();

        login(session).andExpect(redirectedUrl(FAILURE_URL));

        assertThat(em.createNativeQuery("select count(*) from users").getSingleResult())
                .hasToString("0");
        mvc.perform(get("/api/v1/kakao-login-test/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void 카카오에서_취소하면_실패_화면으로_간다() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String state = stateOf(startLogin(session));

        mvc.perform(get("/api/v1/auth/callback/kakao")
                        .param("error", "access_denied")
                        .param("error_description", "User denied access")
                        .param("state", state)
                        .session(session))
                .andExpect(redirectedUrl(FAILURE_URL));
    }

    @Test
    void state가_다르면_실패_화면으로_간다() throws Exception {
        MockHttpSession session = new MockHttpSession();
        startLogin(session);

        mvc.perform(get("/api/v1/auth/callback/kakao")
                        .param("code", "code-1")
                        .param("state", "wrong")
                        .session(session))
                .andExpect(redirectedUrl(FAILURE_URL));
    }

    @Test
    void 카카오의_토큰_호출이_실패하면_실패_화면으로_간다() throws Exception {
        tokenStatus = 401;

        login(new MockHttpSession()).andExpect(redirectedUrl(FAILURE_URL));

        assertThat(em.createNativeQuery("select count(*) from users").getSingleResult())
                .hasToString("0");
    }

    @Test
    void 로그인_뒤_서버에_카카오_토큰이_남지_않는다() throws Exception {
        MockHttpSession session = new MockHttpSession();
        login(session);
        Object userId = em.createNativeQuery("select id from users where kakao_id = '1001'")
                .getSingleResult();

        assertThat(Collections.list(session.getAttributeNames())).containsExactly("SPRING_SECURITY_CONTEXT");
        authorizedClients.ifAvailable(
                service -> assertThat((Object) service.loadAuthorizedClient("kakao", String.valueOf(userId)))
                        .isNull());
    }

    @Test
    void 회원을_만들다_난_오류의_내용은_로그에_남기지_않는다(CapturedOutput output) throws Exception {
        doThrow(new DataIntegrityViolationException("Duplicate entry '1001' for key 'users.uk_users_kakao_id'"))
                .when(registration)
                .findOrRegister(anyString(), anyString());

        login(new MockHttpSession()).andExpect(redirectedUrl(FAILURE_URL));

        assertThat(output)
                .contains("카카오 로그인 실패", "DataIntegrityViolationException")
                .doesNotContain("Duplicate entry");
    }

    @Test
    void 로그인_전_세션은_10분만_남고_로그인하면_14일로_늘어난다() throws Exception {
        MockHttpSession session = new MockHttpSession();

        startLogin(session);
        assertThat(session.getMaxInactiveInterval()).isEqualTo(TEN_MINUTES);

        login(session);
        assertThat(session.getMaxInactiveInterval()).isEqualTo(FOURTEEN_DAYS);
    }

    @Test
    void 로그인한_세션으로_로그인을_다시_시작해도_세션이_짧아지지_않는다() throws Exception {
        MockHttpSession session = new MockHttpSession();
        login(session);

        startLogin(session);

        assertThat(session.getMaxInactiveInterval()).isEqualTo(FOURTEEN_DAYS);
    }

    @Test
    void 로그인하지_않은_요청은_로그인_화면으로_보내지_않고_401을_준다() throws Exception {
        mvc.perform(get("/api/v1/kakao-login-test/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(get("/login"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    private String startLogin(MockHttpSession session) throws Exception {
        return mvc.perform(get("/api/v1/auth/login/kakao").session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();
    }

    private String stateOf(String authorizationUrl) {
        return decode(UriComponentsBuilder.fromUriString(authorizationUrl)
                .build()
                .getQueryParams()
                .getFirst("state"));
    }

    private ResultActions login(MockHttpSession session) throws Exception {
        String state = stateOf(startLogin(session));
        return mvc.perform(get("/api/v1/auth/callback/kakao")
                .param("code", "code-1")
                .param("state", state)
                .session(session));
    }

    private static String formValue(String form, String name) {
        return Arrays.stream(form.split("&"))
                .map(pair -> pair.split("=", 2))
                .filter(pair -> pair[0].equals(name))
                .map(pair -> decode(pair[1]))
                .findFirst()
                .orElse(null);
    }

    private static String sha256(String value) throws NoSuchAlgorithmException {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String user(long id, String nickname) {
        return "{\"id\":" + id + ",\"kakao_account\":{\"profile\":{\"nickname\":\"" + nickname + "\"}}}";
    }

    private static HttpServer startFakeKakao() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/oauth/token", exchange -> {
                tokenRequestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                respond(exchange, tokenStatus, tokenStatus == 200 ? TOKEN_JSON : TOKEN_ERROR_JSON);
            });
            server.createContext("/v2/user/me", exchange -> respond(exchange, 200, userJson));
            server.start();
            return server;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void respond(HttpExchange exchange, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    @RestController
    static class ProbeController {

        @GetMapping("/api/v1/kakao-login-test/me")
        Long me(@AuthenticationPrincipal LoginUser loginUser) {
            return loginUser.userId();
        }
    }
}
