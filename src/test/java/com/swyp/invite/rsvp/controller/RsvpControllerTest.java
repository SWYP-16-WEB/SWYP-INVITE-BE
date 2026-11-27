package com.swyp.invite.rsvp.controller;

import static com.swyp.invite.global.security.LoginTestSupport.loginAs;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.swyp.invite.user.entity.User;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RsvpController.class)
@Import({SecurityConfig.class, RsvpService.class, EventTimePolicy.class})
class RsvpControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EventRepository eventRepository;

    @MockitoBean
    private RsvpRepository rsvpRepository;

    @MockitoBean
    private Clock clock;

    private Event event;

    @BeforeEach
    void setUp() {
        User host = new User("1001", "모임장");
        ReflectionTestUtils.setField(host, "id", 7L);
        event = Event.builder()
                .hostUser(host)
                .shareCode("event123")
                .eventType(EventType.BIRTHDAY)
                .eventAt(LocalDateTime.of(2027, 1, 16, 18, 30))
                .title("생일 파티")
                .place("우리 집")
                .build();
        given(clock.instant()).willReturn(Instant.parse("2027-01-17T03:00:00Z"));
    }

    @Test
    void 본인_이벤트의_참석현황만_명세의_형식으로_반환한다() throws Exception {
        given(eventRepository.findById(15L)).willReturn(Optional.of(event));
        given(rsvpRepository.findAllByEventIdOrderByCreatedAtDescIdDesc(15L))
                .willReturn(List.of(
                        response("응답자A", Attendance.ATTENDING, "guest@example.com", "2027-01-17T02:00:00"),
                        response("응답자B", Attendance.DECLINED, null, "2027-01-17T01:00:00")));

        mvc.perform(get("/api/v1/events/15/rsvps").with(loginAs(7L)))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "attendingCount": 1,
                          "declinedCount": 1,
                          "rsvps": [
                            {"name":"응답자A","email":"guest@example.com","attendance":"ATTENDING",
                             "respondedAt":"2027-01-17T02:00:00+09:00"},
                            {"name":"응답자B","email":null,"attendance":"DECLINED",
                             "respondedAt":"2027-01-17T01:00:00+09:00"}
                          ]
                        }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void 응답이_없으면_0과_빈_목록을_반환한다() throws Exception {
        given(eventRepository.findById(15L)).willReturn(Optional.of(event));
        given(rsvpRepository.findAllByEventIdOrderByCreatedAtDescIdDesc(15L)).willReturn(List.of());

        mvc.perform(get("/api/v1/events/15/rsvps").with(loginAs(7L)))
                .andExpect(status().isOk())
                .andExpect(content()
                        .json("{\"attendingCount\":0,\"declinedCount\":0,\"rsvps\":[]}", JsonCompareMode.STRICT));
    }

    @Test
    void 로그인하지_않으면_컨트롤러_전에_401로_차단한다() throws Exception {
        mvc.perform(get("/api/v1/events/15/rsvps"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        verifyNoInteractions(eventRepository, rsvpRepository);
    }

    @Test
    void 회원_ID를_쿼리로_위조해도_세션_회원이_다르면_403이다() throws Exception {
        given(eventRepository.findById(15L)).willReturn(Optional.of(event));

        mvc.perform(get("/api/v1/events/15/rsvps").param("userId", "7").with(loginAs(8L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verifyNoInteractions(rsvpRepository);
    }

    @Test
    void 비회원_호스트의_이벤트는_403이다() throws Exception {
        ReflectionTestUtils.setField(event, "hostUser", null);
        given(eventRepository.findById(15L)).willReturn(Optional.of(event));

        mvc.perform(get("/api/v1/events/15/rsvps").with(loginAs(7L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verifyNoInteractions(rsvpRepository);
    }

    @Test
    void 없는_이벤트는_404와_공통_에러_응답을_준다() throws Exception {
        given(eventRepository.findById(15L)).willReturn(Optional.empty());

        mvc.perform(get("/api/v1/events/15/rsvps").with(loginAs(7L)))
                .andExpect(status().isNotFound())
                .andExpect(content().json("""
                        {"code":"EVENT_NOT_FOUND","message":"존재하지 않는 이벤트입니다","errors":[]}
                        """, JsonCompareMode.STRICT));

        verifyNoInteractions(rsvpRepository);
    }

    @Test
    void 정확히_48시간부터_이메일을_null로_반환한다() throws Exception {
        given(clock.instant()).willReturn(Instant.parse("2027-01-18T09:30:00Z"));
        given(eventRepository.findById(15L)).willReturn(Optional.of(event));
        given(rsvpRepository.findAllByEventIdOrderByCreatedAtDescIdDesc(15L))
                .willReturn(
                        List.of(response("응답자A", Attendance.ATTENDING, "guest@example.com", "2027-01-17T02:00:00")));

        mvc.perform(get("/api/v1/events/15/rsvps").with(loginAs(7L)))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"attendingCount":1,"declinedCount":0,"rsvps":[
                          {"name":"응답자A","email":null,"attendance":"ATTENDING",
                           "respondedAt":"2027-01-17T02:00:00+09:00"}
                        ]}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void 정확히_1년부터_응답을_읽지_않고_404로_차단한다() throws Exception {
        given(clock.instant()).willReturn(Instant.parse("2028-01-16T09:30:00Z"));
        given(eventRepository.findById(15L)).willReturn(Optional.of(event));

        mvc.perform(get("/api/v1/events/15/rsvps").with(loginAs(7L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"));

        verifyNoInteractions(rsvpRepository);
    }

    private Rsvp response(String name, Attendance attendance, String email, String createdAt) {
        Rsvp rsvp = new Rsvp(event, name, attendance, email, email == null ? null : ReminderStatus.PENDING);
        ReflectionTestUtils.setField(rsvp, "createdAt", LocalDateTime.parse(createdAt));
        return rsvp;
    }
}
