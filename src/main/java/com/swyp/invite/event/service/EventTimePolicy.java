package com.swyp.invite.event.service;

import com.swyp.invite.event.entity.Event;
import com.swyp.invite.global.TimeConfig;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventTimePolicy {

    private final Clock clock;

    public Status evaluate(Event event) {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), TimeConfig.KST);
        LocalDate eventDate = event.getEventAt().toLocalDate();
        LocalDate deadline = event.getRsvpDeadline();
        LocalDate closingDate = deadline == null || deadline.isAfter(eventDate) ? eventDate : deadline;
        boolean expired = !now.isBefore(event.getEventAt().plusHours(48));
        boolean rsvpOpen = !expired && !now.toLocalDate().isAfter(closingDate);
        return new Status(expired, rsvpOpen);
    }

    public record Status(boolean expired, boolean rsvpOpen) {}
}
