package com.swyp.invite.rsvp.service;

import com.swyp.invite.event.entity.Event;
import com.swyp.invite.event.repository.EventRepository;
import com.swyp.invite.event.service.EventTimePolicy;
import com.swyp.invite.global.error.ApiException;
import com.swyp.invite.global.error.ErrorCode;
import com.swyp.invite.rsvp.dto.RsvpCreateRequest;
import com.swyp.invite.rsvp.dto.RsvpResponse;
import com.swyp.invite.rsvp.dto.RsvpSummaryResponse;
import com.swyp.invite.rsvp.entity.Attendance;
import com.swyp.invite.rsvp.entity.ReminderStatus;
import com.swyp.invite.rsvp.entity.Rsvp;
import com.swyp.invite.rsvp.repository.RsvpRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RsvpService {

    private final EventRepository eventRepository;
    private final RsvpRepository rsvpRepository;
    private final EventTimePolicy eventTimePolicy;

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

    @Transactional(readOnly = true)
    public RsvpSummaryResponse getRsvps(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new ApiException(ErrorCode.EVENT_NOT_FOUND));
        if (event.getHostUser() == null || !event.getHostUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        EventTimePolicy.Status status = eventTimePolicy.evaluate(event);
        if (status.retentionEnded()) {
            throw new ApiException(ErrorCode.EVENT_NOT_FOUND);
        }
        List<Rsvp> rsvps = rsvpRepository.findAllByEventIdOrderByCreatedAtDescIdDesc(eventId);
        long attendingCount = rsvps.stream()
                .filter(rsvp -> rsvp.getAttendance() == Attendance.ATTENDING)
                .count();
        long declinedCount = rsvps.stream()
                .filter(rsvp -> rsvp.getAttendance() == Attendance.DECLINED)
                .count();
        List<RsvpResponse> responses = rsvps.stream()
                .map(rsvp -> RsvpResponse.from(rsvp, status.expired()))
                .toList();
        return new RsvpSummaryResponse(attendingCount, declinedCount, responses);
    }
}
