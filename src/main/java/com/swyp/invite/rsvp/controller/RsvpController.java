package com.swyp.invite.rsvp.controller;

import com.swyp.invite.rsvp.dto.RsvpCreateRequest;
import com.swyp.invite.rsvp.service.RsvpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invitations")
@RequiredArgsConstructor
public class RsvpController {

    private final RsvpService rsvpService;

    @PostMapping("/{shareCode}/rsvps")
    @ResponseStatus(HttpStatus.CREATED)
    public void createRsvp(@PathVariable String shareCode, @Valid @RequestBody RsvpCreateRequest request) {
        rsvpService.createRsvp(shareCode, request);
    }
}
