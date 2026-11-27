package com.swyp.invite.rsvp.dto;

import com.swyp.invite.global.TimeConfig;
import com.swyp.invite.rsvp.entity.Attendance;
import com.swyp.invite.rsvp.entity.Rsvp;
import java.time.OffsetDateTime;

public record RsvpResponse(String name, String email, Attendance attendance, OffsetDateTime respondedAt) {

    public static RsvpResponse from(Rsvp rsvp, boolean hideEmail) {
        return new RsvpResponse(
                rsvp.getName(),
                hideEmail ? null : rsvp.getEmail(),
                rsvp.getAttendance(),
                rsvp.getCreatedAt().atZone(TimeConfig.KST).toOffsetDateTime());
    }
}
