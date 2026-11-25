package com.swyp.invite.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.swyp.invite.event.dto.InvitationResponse;
import com.swyp.invite.event.entity.Event;
import com.swyp.invite.global.EntityTestSupport;
import com.swyp.invite.global.error.ApiException;
import com.swyp.invite.global.error.ErrorCode;
import com.swyp.invite.user.entity.User;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class InvitationServiceTest extends EntityTestSupport {

    @Autowired
    private InvitationService service;

    @MockitoBean
    private Clock clock;

    @BeforeEach
    void setUp() {
        given(clock.instant()).willReturn(Instant.parse("2027-01-10T09:30:00Z"));
    }

    @Test
    void 회원과_라이트_호스트의_이벤트를_공유_코드로_조회한다() {
        User host = persist(new User("1001", "모임장"));
        persist(memberEvent(host, "member123")
                .eventAt(LocalDateTime.of(2027, 1, 16, 18, 30))
                .build());
        persist(lightEvent("light123")
                .eventAt(LocalDateTime.of(2027, 1, 20, 12, 0))
                .rsvpDeadline(LocalDate.of(2027, 1, 9))
                .build());
        em.flush();
        em.clear();

        InvitationResponse member = service.getInvitation("member123");
        InvitationResponse light = service.getInvitation("light123");

        assertThat(member.title()).isEqualTo("생일 파티");
        assertThat(member.eventAt().getOffset()).isEqualTo(ZoneOffset.ofHours(9));
        assertThat(member.rsvpOpen()).isTrue();
        assertThat(light.eventAt().toLocalDate()).isEqualTo(LocalDate.of(2027, 1, 20));
        assertThat(light.rsvpOpen()).isFalse();
    }

    @Test
    void 없는_공유_코드는_INVITATION_NOT_FOUND다() {
        assertThatThrownBy(() -> service.getInvitation("missing"))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVITATION_NOT_FOUND));
    }

    @Test
    void 만료_작업이_아직_실행되지_않아도_만료된_초대장은_거절한다() {
        Event event = persist(lightEvent("expired123")
                .eventAt(LocalDateTime.of(2027, 1, 8, 18, 30))
                .settlementInfo("가짜은행 000-000-000000")
                .build());
        em.flush();
        em.clear();

        assertThatThrownBy(() -> service.getInvitation("expired123"))
                .isInstanceOfSatisfying(
                        ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.EVENT_EXPIRED));
        assertThat(raw("select settlement_info from events where id = ?", event.getId()))
                .isNotNull();
    }
}
