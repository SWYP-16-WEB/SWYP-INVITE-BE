package com.swyp.invite.rsvp.controller;

import static com.swyp.invite.global.security.LoginTestSupport.loginAs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.swyp.invite.event.entity.Event;
import com.swyp.invite.event.entity.EventType;
import com.swyp.invite.event.repository.EventRepository;
import com.swyp.invite.event.service.EventTimePolicy;
import com.swyp.invite.global.security.SecurityConfig;
import com.swyp.invite.rsvp.entity.Attendance;
import com.swyp.invite.rsvp.entity.ReminderStatus;
import com.swyp.invite.rsvp.entity.Rsvp;
import com.swyp.invite.rsvp.repository.RsvpRepository;
import com.swyp.invite.rsvp.service.RsvpService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RsvpController.class)
@Import({SecurityConfig.class, RsvpService.class, EventTimePolicy.class})
class RsvpCreateControllerTest {

    private static final String PATH = "/api/v1/invitations/abc123/rsvps";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EventRepository eventRepository;

    @MockitoBean
    private RsvpRepository rsvpRepository;

    @MockitoBean
    private Clock clock;

    @BeforeEach
    void setUp() {
        given(clock.instant()).willReturn(Instant.parse("2027-01-10T09:30:00Z"));
    }

    @Test
    void 로그인과_CSRF_토큰_없이_등록하고_본문_없는_201을_준다() throws Exception {
        given(eventRepository.findByShareCode("abc123")).willReturn(Optional.of(event().build()));

        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"  하늘  ","attendance":"ATTENDING","email":"  sky@example.com  "}
                        """))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));

        ArgumentCaptor<Rsvp> saved = ArgumentCaptor.forClass(Rsvp.class);
        verify(rsvpRepository).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("하늘");
        assertThat(saved.getValue().getEmail()).isEqualTo("sky@example.com");
        assertThat(saved.getValue().getAttendance()).isEqualTo(Attendance.ATTENDING);
        assertThat(saved.getValue().getReminderStatus()).isEqualTo(ReminderStatus.PENDING);
    }

    @Test
    void 로그인한_회원도_이메일_없이_불참_응답을_등록한다() throws Exception {
        given(eventRepository.findByShareCode("abc123")).willReturn(Optional.of(event().build()));

        mvc.perform(post(PATH)
                        .with(loginAs(7L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"name":"하늘","attendance":"DECLINED"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));

        ArgumentCaptor<Rsvp> saved = ArgumentCaptor.forClass(Rsvp.class);
        verify(rsvpRepository).save(saved.capture());
        assertThat(saved.getValue().getAttendance()).isEqualTo(Attendance.DECLINED);
        assertThat(saved.getValue().getEmail()).isNull();
        assertThat(saved.getValue().getReminderStatus()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "\"\"", "\"   \""})
    void 비어_있는_이메일은_입력하지_않은_것으로_받는다(String email) throws Exception {
        given(eventRepository.findByShareCode("abc123")).willReturn(Optional.of(event().build()));

        mvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"하늘\",\"attendance\":\"ATTENDING\",\"email\":" + email + "}"))
                .andExpect(status().isCreated());

        ArgumentCaptor<Rsvp> saved = ArgumentCaptor.forClass(Rsvp.class);
        verify(rsvpRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isNull();
        assertThat(saved.getValue().getReminderStatus()).isNull();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{}",
                "{\"attendance\":\"ATTENDING\"}",
                "{\"name\":null,\"attendance\":\"ATTENDING\"}",
                "{\"name\":\"   \",\"attendance\":\"ATTENDING\"}",
                "{\"name\":\"하늘\"}",
                "{\"name\":\"하늘\",\"attendance\":null}",
                "{\"name\":\"하늘\",\"attendance\":\"MAYBE\"}",
                "{\"name\":\"하늘\",\"attendance\":\"ATTENDING\",\"email\":\"invalid\"}",
                "{broken"
            })
    void 잘못된_입력은_400이고_저장하지_않는다(String body) throws Exception {
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors").isArray());

        verifyNoInteractions(eventRepository, rsvpRepository);
    }

    @Test
    void 이름_21자와_이메일_81자는_필드별_오류를_준다() throws Exception {
        String body = "{\"name\":\"" + "가".repeat(21)
                + "\",\"attendance\":\"ATTENDING\",\"email\":\"" + "a".repeat(60) + "@" + "b".repeat(16)
                + ".com\"}";

        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[?(@.field == 'name')]").isNotEmpty())
                .andExpect(jsonPath("$.errors[?(@.field == 'email')]").isNotEmpty());

        verifyNoInteractions(eventRepository, rsvpRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "1", "\"0\"", "\"1\""})
    void 참석_여부는_숫자나_숫자_문자열로_받지_않는다(String attendance) throws Exception {
        given(eventRepository.findByShareCode("abc123")).willReturn(Optional.of(event().build()));

        mvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"하늘\",\"attendance\":" + attendance + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(rsvpRepository);
    }

    @Test
    void 이름_20자와_이메일_80자는_받는다() throws Exception {
        given(eventRepository.findByShareCode("abc123")).willReturn(Optional.of(event().build()));
        String body = "{\"name\":\"" + "가".repeat(20)
                + "\",\"attendance\":\"ATTENDING\",\"email\":\"" + "a".repeat(60) + "@" + "b".repeat(15)
                + ".com\"}";

        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        verify(rsvpRepository).save(any(Rsvp.class));
    }

    @Test
    void 없는_초대장은_404이고_저장하지_않는다() throws Exception {
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"하늘","attendance":"ATTENDING"}
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVITATION_NOT_FOUND"))
                .andExpect(jsonPath("$.errors").isEmpty());

        verifyNoInteractions(rsvpRepository);
    }

    @Test
    void 마감된_초대장은_409이고_저장하지_않는다() throws Exception {
        given(eventRepository.findByShareCode("abc123"))
                .willReturn(Optional.of(
                        event().rsvpDeadline(LocalDate.of(2027, 1, 9)).build()));

        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"하늘","attendance":"ATTENDING"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RSVP_CLOSED"))
                .andExpect(jsonPath("$.errors").isEmpty());

        verifyNoInteractions(rsvpRepository);
    }

    private Event.EventBuilder event() {
        return Event.builder()
                .shareCode("abc123")
                .hostCodeHash("test-host-hash")
                .eventType(EventType.BIRTHDAY)
                .title("생일 파티")
                .eventAt(LocalDateTime.of(2027, 1, 16, 18, 30))
                .place("우리 집");
    }
}
