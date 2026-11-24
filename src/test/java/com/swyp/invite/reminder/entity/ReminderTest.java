package com.swyp.invite.reminder.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.swyp.invite.event.entity.Event;
import com.swyp.invite.global.EntityTestSupport;

import jakarta.persistence.PersistenceException;

class ReminderTest extends EntityTestSupport {

	@Test
	void 저장하고_다시_읽으면_값이_같다() {
		Event event = persist(lightEvent("share-1").build());
		Reminder reminder = persist(
				new Reminder(event, LocalDateTime.of(2026, 12, 24, 10, 0), "모임 일정 리마인드", "내일 만나요"));
		em.flush();
		em.clear();

		Reminder found = em.find(Reminder.class, reminder.getId());
		assertThat(found.getEvent().getId()).isEqualTo(event.getId());
		assertThat(found.getRemindAt()).isEqualTo(LocalDateTime.of(2026, 12, 24, 10, 0));
		assertThat(found.getTitle()).isEqualTo("모임 일정 리마인드");
		assertThat(found.getMessage()).isEqualTo("내일 만나요");
	}

	@Test
	void 제목이_비면_저장되지_않는다() {
		Event event = persist(lightEvent("share-1").build());

		assertThatThrownBy(() -> {
			em.persist(new Reminder(event, LocalDateTime.of(2026, 12, 24, 10, 0), null, "내일 만나요"));
			em.flush();
		}).isInstanceOf(PersistenceException.class).hasStackTraceContaining("'title' cannot be null");
	}

	@Test
	void 본문이_비면_저장되지_않는다() {
		Event event = persist(lightEvent("share-1").build());

		assertThatThrownBy(() -> {
			em.persist(new Reminder(event, LocalDateTime.of(2026, 12, 24, 10, 0), "모임 일정 리마인드", null));
			em.flush();
		}).isInstanceOf(PersistenceException.class).hasStackTraceContaining("'message' cannot be null");
	}

	@Test
	void 이벤트_하나에_하나만_저장된다() {
		Event event = persist(lightEvent("share-1").build());
		persist(new Reminder(event, LocalDateTime.of(2026, 12, 24, 10, 0), "모임 일정 리마인드", "내일 만나요"));

		assertThatThrownBy(() -> {
			em.persist(new Reminder(event, LocalDateTime.of(2026, 12, 24, 18, 0), "모임 일정 리마인드", "곧 만나요"));
			em.flush();
		}).isInstanceOf(PersistenceException.class).hasStackTraceContaining("uk_reminders_event_id");
	}

}
