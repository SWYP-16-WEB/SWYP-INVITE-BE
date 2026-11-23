package com.swyp.invite.event.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import com.swyp.invite.global.PiiEncryptionConverter;
import com.swyp.invite.user.entity.User;

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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "host_user_id")
	@OnDelete(action = OnDeleteAction.CASCADE)
	private User hostUser;

	private String hostCodeHash;

	private String shareCode;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	private EventType eventType;

	private String eventTypeCustom;

	private String title;

	private LocalDateTime eventAt;

	private String place;

	private LocalDate rsvpDeadline;

	private String photoUrl;

	private LocalDateTime reminderAt;

	private String reminderMessage;

	@Convert(converter = PiiEncryptionConverter.class)
	private String settlementInfo;

	@CreationTimestamp
	private LocalDateTime createdAt;

	@UpdateTimestamp
	private LocalDateTime updatedAt;

	@Builder
	private Event(User hostUser, String hostCodeHash, String shareCode, EventType eventType, String eventTypeCustom,
			String title, LocalDateTime eventAt, String place, LocalDate rsvpDeadline, String photoUrl,
			String settlementInfo) {
		this.hostUser = hostUser;
		this.hostCodeHash = hostCodeHash;
		this.shareCode = shareCode;
		this.eventType = eventType;
		this.eventTypeCustom = eventTypeCustom;
		this.title = title;
		this.eventAt = eventAt;
		this.place = place;
		this.rsvpDeadline = rsvpDeadline;
		this.photoUrl = photoUrl;
		this.settlementInfo = settlementInfo;
	}

}
