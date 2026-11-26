package com.swyp.invite.rsvp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.swyp.invite.event.entity.Event;
import com.swyp.invite.global.EntityTestSupport;
import com.swyp.invite.global.error.ApiException;
import com.swyp.invite.global.error.ErrorCode;
import com.swyp.invite.rsvp.dto.RsvpCreateRequest;
import com.swyp.invite.rsvp.entity.Attendance;
import com.swyp.invite.rsvp.entity.ReminderStatus;
import com.swyp.invite.rsvp.entity.Rsvp;
import com.swyp.invite.user.entity.User;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class RsvpServiceTest extends EntityTestSupport {

    @Autowired
    private RsvpService service;

    @MockitoBean
    private Clock clock;

    @BeforeEach
    void setUp() {
        given(clock.instant()).willReturn(Instant.parse("2026-12-20T09:30:00Z"));
    }

    @Test
    void 같은_이름도_각각_저장하고_공유_코드의_이벤트에_연결한다() {
        User host = persist(new User("1001", "모임장"));
        Event member = persist(memberEvent(host, "member123").build());
        Event light = persist(lightEvent("light123").build());

        service.createRsvp("member123", new RsvpCreateRequest("하늘", Attendance.ATTENDING, "sky@example.com"));
        service.createRsvp("member123", new RsvpCreateRequest("하늘", Attendance.DECLINED, null));
        service.createRsvp("light123", new RsvpCreateRequest("하늘", Attendance.ATTENDING, null));
        em.flush();
        em.clear();

        var responses =
                em.createQuery("select r from Rsvp r order by r.id", Rsvp.class).getResultList();
        assertThat(responses).hasSize(3);
        assertThat(responses)
                .extracting(r -> r.getEvent().getId())
                .containsExactly(member.getId(), member.getId(), light.getId());
        assertThat(responses)
                .extracting(Rsvp::getAttendance)
                .containsExactly(Attendance.ATTENDING, Attendance.DECLINED, Attendance.ATTENDING);
        assertThat(responses).allSatisfy(r -> {
            assertThat(r.getName()).isEqualTo("하늘");
            assertThat(r.getCreatedAt()).isNotNull();
        });
        assertThat(responses.get(0).getEmail()).isEqualTo("sky@example.com");
        assertThat(responses.get(0).getReminderStatus()).isEqualTo(ReminderStatus.PENDING);
        assertThat(raw("select email from rsvps where id = ?", responses.get(0).getId()))
                .isNotNull()
                .isNotEqualTo("sky@example.com");
        assertThat(responses.get(1).getEmail()).isNull();
        assertThat(responses.get(1).getReminderStatus()).isNull();
    }

    @Test
    void 불참이어도_입력한_이메일과_대기_상태를_보관한다() {
        persist(lightEvent("abc123").build());

        service.createRsvp("abc123", new RsvpCreateRequest("하늘", Attendance.DECLINED, "sky@example.com"));
        em.flush();
        em.clear();

        Rsvp response = em.createQuery("select r from Rsvp r", Rsvp.class).getSingleResult();
        assertThat(response.getAttendance()).isEqualTo(Attendance.DECLINED);
        assertThat(response.getEmail()).isEqualTo("sky@example.com");
        assertThat(response.getReminderStatus()).isEqualTo(ReminderStatus.PENDING);
    }

    @Test
    void 없는_초대장은_저장하지_않는다() {
        assertThatThrownBy(() -> service.createRsvp("missing", request()))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVITATION_NOT_FOUND));
        assertThat(count("rsvps")).isZero();
    }

    @ParameterizedTest
    @CsvSource({
        "2027-01-10, 2027-01-10T14:59:59.999999Z, true",
        "2027-01-10, 2027-01-10T15:00:00Z, false",
        ", 2027-01-16T14:59:59.999999Z, true",
        ", 2027-01-16T15:00:00Z, false",
        "2027-01-20, 2027-01-16T14:59:59.999999Z, true",
        "2027-01-20, 2027-01-16T15:00:00Z, false",
        ", 2027-01-18T09:29:59.999999Z, false",
        ", 2027-01-18T09:30:00Z, false"
    })
    void 마감_및_만료_경계에_맞춰_저장_여부를_결정한다(LocalDate deadline, Instant now, boolean open) {
        persist(lightEvent("abc123")
                .eventAt(LocalDateTime.of(2027, 1, 16, 18, 30))
                .rsvpDeadline(deadline)
                .build());
        given(clock.instant()).willReturn(now);

        if (open) {
            service.createRsvp("abc123", request());
            em.flush();
            assertThat(count("rsvps")).isEqualTo(1);
        } else {
            assertThatThrownBy(() -> service.createRsvp("abc123", request()))
                    .isInstanceOfSatisfying(
                            ApiException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RSVP_CLOSED));
            assertThat(count("rsvps")).isZero();
        }
    }

    private RsvpCreateRequest request() {
        return new RsvpCreateRequest("하늘", Attendance.ATTENDING, null);
    }
}
