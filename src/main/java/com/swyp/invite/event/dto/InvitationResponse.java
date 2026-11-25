package com.swyp.invite.event.dto;

import com.swyp.invite.event.entity.Event;
import com.swyp.invite.event.entity.EventType;
import com.swyp.invite.global.TimeConfig;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record InvitationResponse(
        EventType eventType,
        String customEventType,
        String title,
        OffsetDateTime eventAt,
        String place,
        String photoUrl,
        LocalDate rsvpDeadline,
        boolean rsvpOpen,
        boolean edited) {

    public static InvitationResponse from(Event event, boolean rsvpOpen) {
        return new InvitationResponse(
                event.getEventType(),
                event.getEventType() == EventType.CUSTOM ? event.getEventTypeCustom() : null,
                event.getTitle(),
                event.getEventAt().atZone(TimeConfig.KST).toOffsetDateTime(),
                event.getPlace(),
                event.getPhotoUrl(),
                event.getRsvpDeadline(),
                rsvpOpen,
                event.getEditedAt() != null);
    }
}
