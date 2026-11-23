package com.swyp.invite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import com.swyp.invite.checklist.entity.ChecklistItem;
import com.swyp.invite.event.entity.Event;
import com.swyp.invite.event.entity.EventType;
import com.swyp.invite.reminder.entity.Reminder;
import com.swyp.invite.rsvp.entity.Attendance;
import com.swyp.invite.rsvp.entity.ReminderStatus;
import com.swyp.invite.rsvp.entity.Rsvp;
import com.swyp.invite.user.entity.User;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class EntityMappingTest {

	@Autowired
	EntityManager em;

	@Test
	void 저장하고_다시_읽으면_값이_같다() {
		User user = persist(new User("kakao-1", "닉네임"));
		Event event = persist(memberEvent(user, "share-1").rsvpDeadline(LocalDate.of(2026, 12, 20)).build());
		ChecklistItem item = persist(new ChecklistItem(event, "케이크", true));
		Rsvp rsvp = persist(new Rsvp(event, "게스트", Attendance.ATTENDING, "user1@example.com", ReminderStatus.PENDING));
		em.flush();
		em.clear();

		Event foundEvent = em.find(Event.class, event.getId());
		assertThat(foundEvent.getHostUser().getKakaoId()).isEqualTo("kakao-1");
		assertThat(foundEvent.getHostUser().getNickname()).isEqualTo("닉네임");
		assertThat(foundEvent.getEventType()).isEqualTo(EventType.BIRTHDAY);
		assertThat(foundEvent.getTitle()).isEqualTo("생일 파티");
		assertThat(foundEvent.getPlace()).isEqualTo("우리 집");
		assertThat(foundEvent.getEventAt()).isEqualTo(LocalDateTime.of(2026, 12, 25, 19, 0));
		assertThat(foundEvent.getRsvpDeadline()).isEqualTo(LocalDate.of(2026, 12, 20));
		assertThat(foundEvent.getCreatedAt()).isNotNull();
		assertThat(foundEvent.getUpdatedAt()).isNotNull();

		ChecklistItem foundItem = em.find(ChecklistItem.class, item.getId());
		assertThat(foundItem.getContent()).isEqualTo("케이크");
		assertThat(foundItem.isNeedsPurchase()).isTrue();
		assertThat(foundItem.isDone()).isFalse();

		Rsvp foundRsvp = em.find(Rsvp.class, rsvp.getId());
		assertThat(foundRsvp.getName()).isEqualTo("게스트");
		assertThat(foundRsvp.getAttendance()).isEqualTo(Attendance.ATTENDING);
		assertThat(foundRsvp.getReminderStatus()).isEqualTo(ReminderStatus.PENDING);
		assertThat(foundRsvp.getCreatedAt()).isNotNull();
	}

	@Test
	void enum은_이름으로_시각은_그대로_저장한다() {
		Event event = persist(lightEvent("share-1").build());
		Rsvp rsvp = persist(new Rsvp(event, "게스트", Attendance.DECLINED, null, null));
		em.flush();

		assertThat(raw("select event_type from events where id = ?1", event.getId())).isEqualTo("BIRTHDAY");
		assertThat(raw("select cast(event_at as char) from events where id = ?1", event.getId()))
				.isEqualTo("2026-12-25 19:00:00");
		assertThat(raw("select attendance from rsvps where id = ?1", rsvp.getId())).isEqualTo("DECLINED");
	}

	@Test
	void 회원을_지우면_이벤트와_응답과_체크리스트와_리마인드가_함께_지워진다() {
		User user = persist(new User("kakao-1", "닉네임"));
		Event event = persist(memberEvent(user, "share-1").build());
		persist(new ChecklistItem(event, "케이크", true));
		persist(new Rsvp(event, "게스트", Attendance.ATTENDING, "user1@example.com", ReminderStatus.PENDING));
		persist(new Reminder(event, LocalDateTime.of(2026, 12, 24, 10, 0), "내일 만나요"));
		em.flush();
		em.clear();

		em.remove(em.find(User.class, user.getId()));
		em.flush();

		assertThat(count("events")).isZero();
		assertThat(count("checklist_items")).isZero();
		assertThat(count("rsvps")).isZero();
		assertThat(count("reminders")).isZero();
	}

	@Test
	void 리마인드를_저장하고_다시_읽으면_값이_같다() {
		Event event = persist(lightEvent("share-1").build());
		Reminder reminder = persist(new Reminder(event, LocalDateTime.of(2026, 12, 24, 10, 0), "내일 만나요"));
		em.flush();
		em.clear();

		Reminder found = em.find(Reminder.class, reminder.getId());
		assertThat(found.getEvent().getId()).isEqualTo(event.getId());
		assertThat(found.getRemindAt()).isEqualTo(LocalDateTime.of(2026, 12, 24, 10, 0));
		assertThat(found.getMessage()).isEqualTo("내일 만나요");
	}

	@Test
	void 이벤트_하나에_리마인드는_하나만_저장된다() {
		Event event = persist(lightEvent("share-1").build());
		persist(new Reminder(event, LocalDateTime.of(2026, 12, 24, 10, 0), null));

		assertThatThrownBy(() -> {
			em.persist(new Reminder(event, LocalDateTime.of(2026, 12, 24, 18, 0), null));
			em.flush();
		}).isInstanceOf(PersistenceException.class).hasStackTraceContaining("uk_reminders_event_id");
	}

	@Test
	void share_code가_비면_저장되지_않는다() {
		assertThatThrownBy(() -> {
			em.persist(lightEvent(null).build());
			em.flush();
		}).isInstanceOf(PersistenceException.class).hasStackTraceContaining("share_code");
	}

	@Test
	void share_code가_겹치면_저장되지_않는다() {
		persist(lightEvent("share-1").build());

		assertThatThrownBy(() -> {
			em.persist(lightEvent("share-1").hostCodeHash("hash-2").build());
			em.flush();
		}).isInstanceOf(PersistenceException.class).hasStackTraceContaining("uk_events_share_code");
	}

	@Test
	void 호스트가_없으면_저장되지_않는다() {
		assertThatThrownBy(() -> {
			em.persist(event("share-1").build());
			em.flush();
		}).isInstanceOf(PersistenceException.class).hasStackTraceContaining("ck_events_one_host");
	}

	@Test
	void 회원과_관리_코드가_둘_다_있으면_저장되지_않는다() {
		User user = persist(new User("kakao-1", "닉네임"));

		assertThatThrownBy(() -> {
			em.persist(memberEvent(user, "share-1").hostCodeHash("hash-1").build());
			em.flush();
		}).isInstanceOf(PersistenceException.class).hasStackTraceContaining("ck_events_one_host");
	}

	@Test
	void host_code_hash가_겹치면_저장되지_않는다() {
		persist(lightEvent("share-1").hostCodeHash("hash-1").build());

		assertThatThrownBy(() -> {
			em.persist(lightEvent("share-2").hostCodeHash("hash-1").build());
			em.flush();
		}).isInstanceOf(PersistenceException.class).hasStackTraceContaining("uk_events_host_code_hash");
	}

	@Test
	void kakao_id가_겹치면_저장되지_않는다() {
		persist(new User("kakao-1", "닉네임"));

		assertThatThrownBy(() -> {
			em.persist(new User("kakao-1", "다른 닉네임"));
			em.flush();
		}).isInstanceOf(PersistenceException.class).hasStackTraceContaining("uk_users_kakao_id");
	}

	@Test
	void 이메일과_정산_정보는_암호화해서_저장한다() {
		Event event = persist(lightEvent("share-1").settlementInfo("가짜은행 000-000-000000").build());
		Rsvp rsvp = persist(new Rsvp(event, "게스트", Attendance.ATTENDING, "user1@example.com", ReminderStatus.PENDING));
		em.flush();
		em.clear();

		assertThat(raw("select settlement_info from events where id = ?1", event.getId()))
				.isNotEqualTo("가짜은행 000-000-000000");
		assertThat(raw("select email from rsvps where id = ?1", rsvp.getId())).isNotEqualTo("user1@example.com");
		assertThat(em.find(Event.class, event.getId()).getSettlementInfo()).isEqualTo("가짜은행 000-000-000000");
		assertThat(em.find(Rsvp.class, rsvp.getId()).getEmail()).isEqualTo("user1@example.com");
	}

	@Test
	void 원문_356바이트가_두_암호화_컬럼에_들어간다() {
		String longest = "a".repeat(356);
		Event event = persist(lightEvent("share-1").settlementInfo(longest).build());
		Rsvp rsvp = persist(new Rsvp(event, "게스트", Attendance.ATTENDING, longest, ReminderStatus.PENDING));
		em.flush();
		em.clear();

		assertThat(raw("select char_length(settlement_info) from events where id = ?1", event.getId()))
				.hasToString("512");
		assertThat(raw("select char_length(email) from rsvps where id = ?1", rsvp.getId())).hasToString("512");
		assertThat(em.find(Event.class, event.getId()).getSettlementInfo()).isEqualTo(longest);
		assertThat(em.find(Rsvp.class, rsvp.getId()).getEmail()).isEqualTo(longest);
	}

	private <T> T persist(T entity) {
		em.persist(entity);
		return entity;
	}

	private Event.EventBuilder memberEvent(User user, String shareCode) {
		return event(shareCode).hostUser(user);
	}

	private Event.EventBuilder lightEvent(String shareCode) {
		return event(shareCode).hostCodeHash("hash-of-" + shareCode);
	}

	private Event.EventBuilder event(String shareCode) {
		return Event.builder()
				.shareCode(shareCode)
				.eventType(EventType.BIRTHDAY)
				.title("생일 파티")
				.eventAt(LocalDateTime.of(2026, 12, 25, 19, 0))
				.place("우리 집");
	}

	private Object raw(String sql, Long id) {
		return em.createNativeQuery(sql).setParameter(1, id).getSingleResult();
	}

	private long count(String table) {
		return ((Number) em.createNativeQuery("select count(*) from " + table).getSingleResult()).longValue();
	}

}
