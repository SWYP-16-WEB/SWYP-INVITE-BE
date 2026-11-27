package com.swyp.invite.rsvp.controller;

import com.swyp.invite.global.security.LoginUser;
import com.swyp.invite.rsvp.dto.RsvpSummaryResponse;
import com.swyp.invite.rsvp.service.RsvpService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events/{eventId}/rsvps")
@RequiredArgsConstructor
public class RsvpController {

    private final RsvpService rsvpService;

    @GetMapping
    public RsvpSummaryResponse getRsvps(@PathVariable Long eventId, @AuthenticationPrincipal LoginUser loginUser) {
        return rsvpService.getRsvps(eventId, loginUser.userId());
    }
}
