package com.swyp.invite.event.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.swyp.invite.global.EntityTestSupport;
import com.swyp.invite.user.entity.User;
import jakarta.persistence.PersistenceException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class EventTest extends EntityTestSupport {

    @Test
    void 저장하고_다시_읽으면_값이_같다() {
        User user = persist(new User("kakao-1", "닉네임"));
        Event event = persist(memberEvent(user, "share-1")
                .rsvpDeadline(LocalDate.of(2026, 12, 20))
                .build());
        em.flush();
        em.clear();

        Event found = em.find(Event.class, event.getId());
        assertThat(found.getHostUser().getKakaoId()).isEqualTo("kakao-1");
        assertThat(found.getHostUser().getNickname()).isEqualTo("닉네임");
        assertThat(found.getHostUser().getEmail()).isNull();
        assertThat(found.getHostUser().getSettlementInfo()).isNull();
        assertThat(found.getEventType()).isEqualTo(EventType.BIRTHDAY);
        assertThat(found.getTitle()).isEqualTo("생일 파티");
        assertThat(found.getPlace()).isEqualTo("우리 집");
        assertThat(found.getEventAt()).isEqualTo(LocalDateTime.of(2026, 12, 25, 19, 0));
        assertThat(found.getRsvpDeadline()).isEqualTo(LocalDate.of(2026, 12, 20));
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
        assertThat(found.getEditedAt()).isNull();
    }

    @Test
    void 유형은_이름으로_시각은_그대로_저장한다() {
        Event event = persist(lightEvent("share-1").build());
        em.flush();

        assertThat(raw("select event_type from events where id = ?1", event.getId()))
                .isEqualTo("BIRTHDAY");
        assertThat(raw("select cast(event_at as char) from events where id = ?1", event.getId()))
                .isEqualTo("2026-12-25 19:00:00");
    }

    @Test
    void 수정_시각을_넣으면_그대로_읽힌다() {
        Event event = persist(lightEvent("share-1").build());
        em.flush();
        em.createNativeQuery("update events set edited_at = '2026-12-01 10:00:00' where id = ?1")
                .setParameter(1, event.getId())
                .executeUpdate();
        em.clear();

        assertThat(em.find(Event.class, event.getId()).getEditedAt()).isEqualTo(LocalDateTime.of(2026, 12, 1, 10, 0));
    }

    @Test
    void share_code가_비면_저장되지_않는다() {
        assertThatThrownBy(() -> {
                    em.persist(lightEvent(null).build());
                    em.flush();
                })
                .isInstanceOf(PersistenceException.class)
                .hasStackTraceContaining("share_code");
    }

    @Test
    void share_code가_겹치면_저장되지_않는다() {
        persist(lightEvent("share-1").build());

        assertThatThrownBy(() -> {
                    em.persist(lightEvent("share-1").hostCodeHash("hash-2").build());
                    em.flush();
                })
                .isInstanceOf(PersistenceException.class)
                .hasStackTraceContaining("uk_events_share_code");
    }

    @Test
    void 호스트가_없으면_저장되지_않는다() {
        assertThatThrownBy(() -> {
                    em.persist(event("share-1").build());
                    em.flush();
                })
                .isInstanceOf(PersistenceException.class)
                .hasStackTraceContaining("ck_events_one_host");
    }

    @Test
    void 회원과_관리_코드가_둘_다_있으면_저장되지_않는다() {
        User user = persist(new User("kakao-1", "닉네임"));

        assertThatThrownBy(() -> {
                    em.persist(
                            memberEvent(user, "share-1").hostCodeHash("hash-1").build());
                    em.flush();
                })
                .isInstanceOf(PersistenceException.class)
                .hasStackTraceContaining("ck_events_one_host");
    }

    @Test
    void host_code_hash가_겹치면_저장되지_않는다() {
        persist(lightEvent("share-1").hostCodeHash("hash-1").build());

        assertThatThrownBy(() -> {
                    em.persist(lightEvent("share-2").hostCodeHash("hash-1").build());
                    em.flush();
                })
                .isInstanceOf(PersistenceException.class)
                .hasStackTraceContaining("uk_events_host_code_hash");
    }

    @Test
    void 정산_정보는_암호화해서_저장한다() {
        Event event = persist(
                lightEvent("share-1").settlementInfo("가짜은행 000-000-000000").build());
        em.flush();
        em.clear();

        assertThat(raw("select settlement_info from events where id = ?1", event.getId()))
                .isNotEqualTo("가짜은행 000-000-000000");
        assertThat(em.find(Event.class, event.getId()).getSettlementInfo()).isEqualTo("가짜은행 000-000-000000");
    }

    @Test
    void 정산_정보_원문_356바이트가_암호화_컬럼에_들어간다() {
        String longest = "a".repeat(356);
        Event event = persist(lightEvent("share-1").settlementInfo(longest).build());
        em.flush();
        em.clear();

        assertThat(raw("select char_length(settlement_info) from events where id = ?1", event.getId()))
                .hasToString("512");
        assertThat(em.find(Event.class, event.getId()).getSettlementInfo()).isEqualTo(longest);
    }
}
