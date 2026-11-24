package com.swyp.invite.rsvp.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.swyp.invite.event.entity.Event;
import com.swyp.invite.global.EntityTestSupport;

class RsvpTest extends EntityTestSupport {

	@Test
	void 저장하고_다시_읽으면_값이_같다() {
		Event event = persist(lightEvent("share-1").build());
		Rsvp rsvp = persist(new Rsvp(event, "게스트", Attendance.ATTENDING, "user1@example.com", ReminderStatus.PENDING));
		em.flush();
		em.clear();

		Rsvp found = em.find(Rsvp.class, rsvp.getId());
		assertThat(found.getName()).isEqualTo("게스트");
		assertThat(found.getAttendance()).isEqualTo(Attendance.ATTENDING);
		assertThat(found.getReminderStatus()).isEqualTo(ReminderStatus.PENDING);
		assertThat(found.getCreatedAt()).isNotNull();
	}

	@Test
	void 참석_여부는_이름으로_저장한다() {
		Event event = persist(lightEvent("share-1").build());
		Rsvp rsvp = persist(new Rsvp(event, "게스트", Attendance.DECLINED, null, null));
		em.flush();

		assertThat(raw("select attendance from rsvps where id = ?1", rsvp.getId())).isEqualTo("DECLINED");
	}

	@Test
	void 이메일은_암호화해서_저장한다() {
		Event event = persist(lightEvent("share-1").build());
		Rsvp rsvp = persist(new Rsvp(event, "게스트", Attendance.ATTENDING, "user1@example.com", ReminderStatus.PENDING));
		em.flush();
		em.clear();

		assertThat(raw("select email from rsvps where id = ?1", rsvp.getId())).isNotEqualTo("user1@example.com");
		assertThat(em.find(Rsvp.class, rsvp.getId()).getEmail()).isEqualTo("user1@example.com");
	}

	@Test
	void 이메일_원문_356바이트가_암호화_컬럼에_들어간다() {
		String longest = "a".repeat(356);
		Event event = persist(lightEvent("share-1").build());
		Rsvp rsvp = persist(new Rsvp(event, "게스트", Attendance.ATTENDING, longest, ReminderStatus.PENDING));
		em.flush();
		em.clear();

		assertThat(raw("select char_length(email) from rsvps where id = ?1", rsvp.getId())).hasToString("512");
		assertThat(em.find(Rsvp.class, rsvp.getId()).getEmail()).isEqualTo(longest);
	}

}
