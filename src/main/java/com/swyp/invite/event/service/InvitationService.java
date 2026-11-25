package com.swyp.invite.event.service;

import com.swyp.invite.event.dto.InvitationResponse;
import com.swyp.invite.event.entity.Event;
import com.swyp.invite.event.repository.EventRepository;
import com.swyp.invite.global.error.ApiException;
import com.swyp.invite.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final EventRepository eventRepository;
    private final EventTimePolicy eventTimePolicy;

    @Transactional(readOnly = true)
    public InvitationResponse getInvitation(String shareCode) {
        Event event = eventRepository
                .findByShareCode(shareCode)
                .orElseThrow(() -> new ApiException(ErrorCode.INVITATION_NOT_FOUND));
        EventTimePolicy.Status status = eventTimePolicy.evaluate(event);
        if (status.expired()) {
            throw new ApiException(ErrorCode.EVENT_EXPIRED);
        }
        return InvitationResponse.from(event, status.rsvpOpen());
    }
}
