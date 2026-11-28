package com.swyp.invite.rsvp.controller;

import com.swyp.invite.global.security.LoginUser;
import com.swyp.invite.rsvp.dto.RsvpCreateRequest;
import com.swyp.invite.rsvp.dto.RsvpSummaryResponse;
import com.swyp.invite.rsvp.service.RsvpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RsvpController {

    private final RsvpService rsvpService;

    @GetMapping("/events/{eventId}/rsvps")
    public RsvpSummaryResponse getRsvps(@PathVariable Long eventId, @AuthenticationPrincipal LoginUser loginUser) {
        return rsvpService.getRsvps(eventId, loginUser.userId());
    }

    @PostMapping("/invitations/{shareCode}/rsvps")
    @ResponseStatus(HttpStatus.CREATED)
    public void createRsvp(@PathVariable String shareCode, @Valid @RequestBody RsvpCreateRequest request) {
        rsvpService.createRsvp(shareCode, request);
    }
}
