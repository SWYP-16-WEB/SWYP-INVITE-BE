package com.swyp.invite.rsvp.dto;

import java.util.List;

public record RsvpSummaryResponse(long attendingCount, long declinedCount, List<RsvpResponse> rsvps) {}
