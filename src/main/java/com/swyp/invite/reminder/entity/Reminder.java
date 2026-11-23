package com.swyp.invite.reminder.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.swyp.invite.event.entity.Event;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reminders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reminder {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "event_id")
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Event event;

	private LocalDateTime remindAt;

	private String message;

	public Reminder(Event event, LocalDateTime remindAt, String message) {
		this.event = event;
		this.remindAt = remindAt;
		this.message = message;
	}

}
