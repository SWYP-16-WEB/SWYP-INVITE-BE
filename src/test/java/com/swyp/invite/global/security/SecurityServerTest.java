package com.swyp.invite.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.swyp.invite.TestcontainersConfiguration;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Import({TestcontainersConfiguration.class, SecurityServerTest.ProbeController.class})
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SecurityServerTest {

    private static final int FOURTEEN_DAYS = 14 * 24 * 60 * 60;

    private final HttpClient http = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private ApplicationContext context;

    @Test
    void 회원_API는_로그인_없이_401과_공통_에러_모양을_준다() throws Exception {
        HttpResponse<String> response = get("/api/v1/users/me");

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("content-type"))
                .hasValueSatisfying(type -> assertThat(type).startsWith("application/json"));
        assertThat(response.body()).contains("\"code\":\"UNAUTHENTICATED\"", "\"errors\":[]");
    }

    @Test
    void 로그인하지_않은_요청은_서버에_세션을_만들지_않는다() throws Exception {
        assertThat(get("/api/v1/users/me").headers().allValues("set-cookie"))
                .noneMatch(cookie -> cookie.startsWith("JSESSIONID="));
    }

    @Test
    void 쓸_수_없는_기본_계정과_비밀번호를_만들지_않는다() {
        assertThat(context.getBeanNamesForType(UserDetailsService.class)).isEmpty();
    }

    @Test
    void 공개_API에서_난_예외는_401로_바뀌지_않는다() throws Exception {
        assertThat(get("/api/v1/invitations/boom").statusCode()).isEqualTo(500);
    }

    @Test
    void 공개_경로_아래의_없는_주소는_404다() throws Exception {
        assertThat(get("/api/v1/invitations/a/b/c").statusCode()).isEqualTo(404);
    }

    @Test
    void 세션_쿠키와_서버의_세션은_14일_동안_유지된다() throws Exception {
        HttpResponse<String> response = get("/api/v1/invitations/session");

        assertThat(response.body()).isEqualTo(String.valueOf(FOURTEEN_DAYS));
        assertThat(response.headers().allValues("set-cookie"))
                .filteredOn(cookie -> cookie.startsWith("JSESSIONID="))
                .singleElement()
                .satisfies(cookie -> assertThat(cookie).contains("Max-Age=" + FOURTEEN_DAYS, "HttpOnly"));
    }

    @Test
    void 세션_쿠키를_실어_다시_요청해도_쿠키의_수명은_늘어나지_않는다() throws Exception {
        String session = get("/api/v1/invitations/session").headers().allValues("set-cookie").stream()
                .filter(cookie -> cookie.startsWith("JSESSIONID="))
                .map(cookie -> cookie.substring(0, cookie.indexOf(';')))
                .findFirst()
                .orElseThrow();

        assertThat(get("/api/v1/invitations/session", "Cookie", session)
                        .headers()
                        .allValues("set-cookie"))
                .noneMatch(cookie -> cookie.startsWith("JSESSIONID="));
    }

    @Test
    void 서버가_모르는_세션_쿠키로_공개_API를_부르면_로그인하지_않은_요청으로_받는다() throws Exception {
        assertThat(get("/api/v1/invitations/session", "Cookie", "JSESSIONID=unknown")
                        .statusCode())
                .isEqualTo(200);
    }

    @Test
    void API_문서_화면은_로그인_없이_열린다() throws Exception {
        assertThat(get("/swagger-ui/index.html").statusCode()).isEqualTo(200);
        assertThat(get("/v3/api-docs").statusCode()).isEqualTo(200);
    }

    @Test
    void API_문서_화면은_값을_바꾸는_요청에_CSRF_토큰을_싣는다() throws Exception {
        assertThat(get("/swagger-ui/swagger-initializer.js").body()).contains("X-XSRF-TOKEN");
    }

    @Test
    void 응답으로_받은_CSRF_쿠키의_값을_헤더에_실어_로그아웃한다() throws Exception {
        String token = get("/api/v1/invitations/session").headers().allValues("set-cookie").stream()
                .filter(cookie -> cookie.startsWith("XSRF-TOKEN="))
                .map(cookie -> cookie.substring("XSRF-TOKEN=".length(), cookie.indexOf(';')))
                .findFirst()
                .orElseThrow();

        HttpResponse<String> response = http.send(
                request("/api/v1/auth/logout")
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .header("Cookie", "XSRF-TOKEN=" + token)
                        .header("X-XSRF-TOKEN", token)
                        .build(),
                BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(204);
    }

    private HttpResponse<String> get(String path, String... headers) throws IOException, InterruptedException {
        HttpRequest.Builder request = request(path);
        if (headers.length > 0) {
            request.headers(headers);
        }
        return http.send(request.build(), BodyHandlers.ofString());
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
    }

    @RestController
    static class ProbeController {

        @GetMapping("/api/v1/invitations/boom")
        void boom() {
            throw new IllegalStateException("boom");
        }

        @GetMapping("/api/v1/invitations/session")
        int session(HttpSession session) {
            return session.getMaxInactiveInterval();
        }
    }
}
