package com.swyp.invite.rsvp.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import com.swyp.invite.event.entity.Event;
import com.swyp.invite.global.PiiEncryptionConverter;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "rsvps")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Rsvp {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "event_id")
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Event event;

	private String name;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	private Attendance attendance;

	@Convert(converter = PiiEncryptionConverter.class)
	private String email;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	private ReminderStatus reminderStatus;

	@CreationTimestamp
	private LocalDateTime createdAt;

	public Rsvp(Event event, String name, Attendance attendance, String email, ReminderStatus reminderStatus) {
		this.event = event;
		this.name = name;
		this.attendance = attendance;
		this.email = email;
		this.reminderStatus = reminderStatus;
	}

}
