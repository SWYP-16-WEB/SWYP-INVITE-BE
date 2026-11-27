package com.swyp.invite.rsvp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.swyp.invite.event.entity.Event;
import com.swyp.invite.global.EntityTestSupport;
import com.swyp.invite.global.TimeConfig;
import com.swyp.invite.global.error.ApiException;
import com.swyp.invite.global.error.ErrorCode;
import com.swyp.invite.rsvp.dto.RsvpResponse;
import com.swyp.invite.rsvp.dto.RsvpSummaryResponse;
import com.swyp.invite.rsvp.entity.Attendance;
import com.swyp.invite.rsvp.entity.ReminderStatus;
import com.swyp.invite.rsvp.entity.Rsvp;
import com.swyp.invite.user.entity.User;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class RsvpServiceTest extends EntityTestSupport {

    private static final LocalDateTime EVENT_AT = LocalDateTime.of(2027, 1, 16, 18, 30);

    @Autowired
    private RsvpService service;

    @MockitoBean
    private Clock clock;

    private User host;

    private Event event;

    @BeforeEach
    void setUp() {
        now("2027-01-17T12:00:00");
        host = persist(new User("1001", "모임장"));
        event = persist(memberEvent(host, "event123").eventAt(EVENT_AT).build());
    }

    @Test
    void 본인_이벤트의_응답만_최신순으로_주고_같은_시각이면_ID가_큰_순서로_준다() {
        Rsvp first = response(event, "같은 이름", Attendance.ATTENDING, "first@example.com");
        Rsvp second = response(event, "같은 이름", Attendance.ATTENDING, "second@example.com");
        Rsvp latest = response(event, "최근 응답", Attendance.DECLINED, "latest@example.com");
        Rsvp oldest = response(event, "오래된 응답", Attendance.ATTENDING, null);
        Event other = persist(memberEvent(host, "other123").eventAt(EVENT_AT).build());
        response(other, "다른 이벤트", Attendance.ATTENDING, "other@example.com");
        em.flush();
        respondedAt(first, "2027-01-17T01:00:00");
        respondedAt(second, "2027-01-17T01:00:00");
        respondedAt(latest, "2027-01-17T02:00:00");
        respondedAt(oldest, "2027-01-16T01:00:00");
        em.clear();

        RsvpSummaryResponse summary = service.getRsvps(event.getId(), host.getId());

        assertThat(summary.attendingCount()).isEqualTo(3);
        assertThat(summary.declinedCount()).isEqualTo(1);
        assertThat(summary.rsvps())
                .extracting(RsvpResponse::email)
                .containsExactly("latest@example.com", "second@example.com", "first@example.com", null);
        assertThat(summary.rsvps().getFirst().respondedAt().toLocalDateTime())
                .isEqualTo(LocalDateTime.of(2027, 1, 17, 2, 0));
        assertThat(summary.rsvps().getFirst().respondedAt().getOffset()).isEqualTo(ZoneOffset.ofHours(9));
    }

    @Test
    void 응답이_없으면_인원은_0이고_목록은_빈_배열이다() {
        em.flush();
        em.clear();

        RsvpSummaryResponse summary = service.getRsvps(event.getId(), host.getId());

        assertThat(summary.attendingCount()).isZero();
        assertThat(summary.declinedCount()).isZero();
        assertThat(summary.rsvps()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"2027-01-18T18:29:59, false", "2027-01-18T18:30:00, true", "2027-01-18T18:30:01, true"})
    void 만료_정리_전이라도_48시간부터_이메일을_숨기고_인원과_응답은_유지한다(String now, boolean hidden) {
        Rsvp rsvp = response(event, "응답자", Attendance.ATTENDING, "guest@example.com");
        em.flush();
        em.clear();
        now(now);

        RsvpSummaryResponse summary = service.getRsvps(event.getId(), host.getId());

        assertThat(summary.attendingCount()).isEqualTo(1);
        assertThat(summary.declinedCount()).isZero();
        assertThat(summary.rsvps().getFirst().name()).isEqualTo("응답자");
        assertThat(summary.rsvps().getFirst().email()).isEqualTo(hidden ? null : "guest@example.com");
        assertThat(raw("select email from rsvps where id = ?", rsvp.getId()))
                .isNotNull()
                .isNotEqualTo("guest@example.com");
    }

    @ParameterizedTest
    @CsvSource({"2028-01-16T18:29:59, false", "2028-01-16T18:30:00, true", "2028-01-16T18:30:01, true"})
    void 삭제_작업_전이라도_행사_시작_후_1년부터_조회할_수_없다(String now, boolean unavailable) {
        response(event, "응답자", Attendance.ATTENDING, "guest@example.com");
        em.flush();
        em.clear();
        now(now);

        if (unavailable) {
            assertThatThrownBy(() -> service.getRsvps(event.getId(), host.getId()))
                    .isInstanceOfSatisfying(
                            ApiException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.EVENT_NOT_FOUND));
        } else {
            RsvpSummaryResponse summary = service.getRsvps(event.getId(), host.getId());
            assertThat(summary.rsvps()).hasSize(1);
            assertThat(summary.rsvps().getFirst().email()).isNull();
        }
        assertThat(count("events")).isEqualTo(1);
    }

    @Test
    void 다른_회원은_조회할_수_없다() {
        User other = persist(new User("1002", "다른 회원"));
        em.flush();
        em.clear();

        assertThatThrownBy(() -> service.getRsvps(event.getId(), other.getId()))
                .isInstanceOfSatisfying(
                        ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void 비회원_호스트의_이벤트는_회원_API로_조회할_수_없다() {
        Event light = persist(lightEvent("light123").eventAt(EVENT_AT).build());
        em.flush();
        em.clear();

        assertThatThrownBy(() -> service.getRsvps(light.getId(), host.getId()))
                .isInstanceOfSatisfying(
                        ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void 없는_이벤트는_EVENT_NOT_FOUND다() {
        assertThatThrownBy(() -> service.getRsvps(999_999L, host.getId()))
                .isInstanceOfSatisfying(
                        ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.EVENT_NOT_FOUND));
    }

    private void now(String value) {
        given(clock.instant())
                .willReturn(LocalDateTime.parse(value).atZone(TimeConfig.KST).toInstant());
    }

    private Rsvp response(Event event, String name, Attendance attendance, String email) {
        return persist(new Rsvp(event, name, attendance, email, email == null ? null : ReminderStatus.PENDING));
    }

    private void respondedAt(Rsvp rsvp, String value) {
        em.createNativeQuery("update rsvps set created_at = ?1 where id = ?2")
                .setParameter(1, LocalDateTime.parse(value))
                .setParameter(2, rsvp.getId())
                .executeUpdate();
    }
}
