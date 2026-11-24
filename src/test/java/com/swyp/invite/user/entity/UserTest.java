package com.swyp.invite.user.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.swyp.invite.checklist.entity.ChecklistItem;
import com.swyp.invite.event.entity.Event;
import com.swyp.invite.global.EntityTestSupport;
import com.swyp.invite.reminder.entity.Reminder;
import com.swyp.invite.rsvp.entity.Attendance;
import com.swyp.invite.rsvp.entity.ReminderStatus;
import com.swyp.invite.rsvp.entity.Rsvp;

import jakarta.persistence.PersistenceException;

class UserTest extends EntityTestSupport {

	@Test
	void kakao_id가_겹치면_저장되지_않는다() {
		persist(new User("kakao-1", "닉네임"));

		assertThatThrownBy(() -> {
			em.persist(new User("kakao-1", "다른 닉네임"));
			em.flush();
		}).isInstanceOf(PersistenceException.class).hasStackTraceContaining("uk_users_kakao_id");
	}

	@Test
	void 이름과_이메일과_정산정보를_고치고_이메일과_정산정보는_암호화해서_저장한다() {
		User user = persist(new User("kakao-1", "닉네임"));
		user.updateProfile("새 이름", "user1@example.com", "가짜은행 000-000-000000");
		em.flush();
		em.clear();

		assertThat(raw("select email from users where id = ?1", user.getId())).isNotEqualTo("user1@example.com");
		assertThat(raw("select settlement_info from users where id = ?1", user.getId()))
				.isNotEqualTo("가짜은행 000-000-000000");
		User found = em.find(User.class, user.getId());
		assertThat(found.getNickname()).isEqualTo("새 이름");
		assertThat(found.getEmail()).isEqualTo("user1@example.com");
		assertThat(found.getSettlementInfo()).isEqualTo("가짜은행 000-000-000000");
	}

	@Test
	void 이메일과_정산정보_원문_356바이트가_암호화_컬럼에_들어간다() {
		String longest = "a".repeat(356);
		User user = persist(new User("kakao-1", "닉네임"));
		user.updateProfile("닉네임", longest, longest);
		em.flush();
		em.clear();

		assertThat(raw("select char_length(email) from users where id = ?1", user.getId())).hasToString("512");
		assertThat(raw("select char_length(settlement_info) from users where id = ?1", user.getId()))
				.hasToString("512");
		assertThat(em.find(User.class, user.getId()).getEmail()).isEqualTo(longest);
		assertThat(em.find(User.class, user.getId()).getSettlementInfo()).isEqualTo(longest);
	}

	@Test
	void 지우면_이벤트와_응답과_체크리스트와_리마인드가_함께_지워진다() {
		User user = persist(new User("kakao-1", "닉네임"));
		Event event = persist(memberEvent(user, "share-1").build());
		persist(new ChecklistItem(event, "케이크", null, null));
		persist(new Rsvp(event, "게스트", Attendance.ATTENDING, "user1@example.com", ReminderStatus.PENDING));
		persist(new Reminder(event, LocalDateTime.of(2026, 12, 24, 10, 0), "모임 일정 리마인드", "내일 만나요"));
		em.flush();
		em.clear();

		em.remove(em.find(User.class, user.getId()));
		em.flush();

		assertThat(count("events")).isZero();
		assertThat(count("checklist_items")).isZero();
		assertThat(count("rsvps")).isZero();
		assertThat(count("reminders")).isZero();
	}

}
