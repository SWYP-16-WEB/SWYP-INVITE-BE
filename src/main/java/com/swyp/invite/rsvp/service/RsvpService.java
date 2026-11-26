package com.swyp.invite.rsvp.service;

import com.swyp.invite.event.entity.Event;
import com.swyp.invite.event.repository.EventRepository;
import com.swyp.invite.event.service.EventTimePolicy;
import com.swyp.invite.global.error.ApiException;
import com.swyp.invite.global.error.ErrorCode;
import com.swyp.invite.rsvp.dto.RsvpCreateRequest;
import com.swyp.invite.rsvp.entity.ReminderStatus;
import com.swyp.invite.rsvp.entity.Rsvp;
import com.swyp.invite.rsvp.repository.RsvpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RsvpService {

    private final EventRepository eventRepository;
    private final EventTimePolicy eventTimePolicy;
    private final RsvpRepository rsvpRepository;

    @Transactional
    public void createRsvp(String shareCode, RsvpCreateRequest request) {
        Event event = eventRepository
                .findByShareCode(shareCode)
                .orElseThrow(() -> new ApiException(ErrorCode.INVITATION_NOT_FOUND));
        if (!eventTimePolicy.evaluate(event).rsvpOpen()) {
            throw new ApiException(ErrorCode.RSVP_CLOSED);
        }

        ReminderStatus reminderStatus = request.email() == null ? null : ReminderStatus.PENDING;
        rsvpRepository.save(new Rsvp(event, request.name(), request.attendance(), request.email(), reminderStatus));
    }
}
