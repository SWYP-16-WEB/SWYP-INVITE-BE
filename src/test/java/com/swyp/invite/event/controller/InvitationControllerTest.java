package com.swyp.invite.event.controller;

import static com.swyp.invite.global.security.LoginTestSupport.loginAs;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.swyp.invite.event.entity.Event;
import com.swyp.invite.event.entity.EventType;
import com.swyp.invite.event.repository.EventRepository;
import com.swyp.invite.event.service.EventTimePolicy;
import com.swyp.invite.event.service.InvitationService;
import com.swyp.invite.global.security.SecurityConfig;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

@WebMvcTest(InvitationController.class)
@Import({SecurityConfig.class, InvitationService.class, EventTimePolicy.class})
class InvitationControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EventRepository eventRepository;

    @MockitoBean
    private Clock clock;

    @BeforeEach
    void setUp() {
        given(clock.instant()).willReturn(Instant.parse("2027-01-10T09:30:00Z"));
    }

    @Test
    void 비회원과_회원에게_동일한_공개_필드_아홉_개만_준다() throws Exception {
        Event event = event().rsvpDeadline(LocalDate.of(2027, 1, 10))
                .photoUrl("https://example.com/photo.png")
                .settlementInfo("가짜은행 000-000-000000")
                .build();
        given(eventRepository.findByShareCode("abc123")).willReturn(Optional.of(event));
        String expected = """
                {
                  "eventType": "BIRTHDAY",
                  "customEventType": null,
                  "title": "생일 파티",
                  "eventAt": "2027-01-16T18:30:00+09:00",
                  "place": "우리 집",
                  "photoUrl": "https://example.com/photo.png",
                  "rsvpDeadline": "2027-01-10",
                  "rsvpOpen": true,
                  "edited": false
                }
                """;

        mvc.perform(get("/api/v1/invitations/abc123"))
                .andExpect(status().isOk())
                .andExpect(content().json(expected, JsonCompareMode.STRICT));
        mvc.perform(get("/api/v1/invitations/abc123").with(loginAs(7L)))
                .andExpect(status().isOk())
                .andExpect(content().json(expected, JsonCompareMode.STRICT));
    }

    @Test
    void 선택_필드가_없어도_null로_유지한다() throws Exception {
        given(eventRepository.findByShareCode("abc123")).willReturn(Optional.of(event().build()));

        mvc.perform(get("/api/v1/invitations/abc123"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                                {
                                  "eventType": "BIRTHDAY",
                                  "customEventType": null,
                                  "title": "생일 파티",
                                  "eventAt": "2027-01-16T18:30:00+09:00",
                                  "place": "우리 집",
                                  "photoUrl": null,
                                  "rsvpDeadline": null,
                                  "rsvpOpen": true,
                                  "edited": false
                                }
                                """, JsonCompareMode.STRICT));
    }

    @Test
    void 직접_입력한_유형과_수정_여부를_준다() throws Exception {
        Event event =
                event().eventType(EventType.CUSTOM).eventTypeCustom("독서 모임").build();
        ReflectionTestUtils.setField(event, "editedAt", LocalDateTime.of(2027, 1, 9, 12, 0));
        given(eventRepository.findByShareCode("abc123")).willReturn(Optional.of(event));

        mvc.perform(get("/api/v1/invitations/abc123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customEventType").value("독서 모임"))
                .andExpect(jsonPath("$.edited").value(true));
    }

    @Test
    void 응답_마감_뒤에도_만료_전이면_조회할_수_있다() throws Exception {
        given(eventRepository.findByShareCode("abc123"))
                .willReturn(Optional.of(
                        event().rsvpDeadline(LocalDate.of(2027, 1, 9)).build()));

        mvc.perform(get("/api/v1/invitations/abc123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rsvpOpen").value(false));
    }

    @Test
    void 없는_공유_코드는_404와_공통_에러_응답을_준다() throws Exception {
        given(eventRepository.findByShareCode("missing")).willReturn(Optional.empty());

        mvc.perform(get("/api/v1/invitations/missing"))
                .andExpect(status().isNotFound())
                .andExpect(content().json("""
                                {"code":"INVITATION_NOT_FOUND","message":"존재하지 않는 초대장입니다","errors":[]}
                                """, JsonCompareMode.STRICT));
    }

    @Test
    void 시작_후_48시간이_되면_410과_공통_에러_응답을_준다() throws Exception {
        given(clock.instant()).willReturn(Instant.parse("2027-01-18T09:30:00Z"));
        given(eventRepository.findByShareCode("abc123")).willReturn(Optional.of(event().build()));

        mvc.perform(get("/api/v1/invitations/abc123"))
                .andExpect(status().isGone())
                .andExpect(content().json("""
                                {"code":"EVENT_EXPIRED","message":"만료된 초대장입니다","errors":[]}
                                """, JsonCompareMode.STRICT));
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
