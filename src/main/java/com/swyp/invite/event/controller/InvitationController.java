package com.swyp.invite.event.controller;

import com.swyp.invite.event.dto.InvitationResponse;
import com.swyp.invite.event.service.InvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @GetMapping("/{shareCode}")
    public InvitationResponse getInvitation(@PathVariable String shareCode) {
        return invitationService.getInvitation(shareCode);
    }
}
