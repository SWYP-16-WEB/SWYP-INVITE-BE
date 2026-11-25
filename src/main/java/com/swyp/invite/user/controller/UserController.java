package com.swyp.invite.user.controller;

import com.swyp.invite.global.security.LoginUser;
import com.swyp.invite.user.dto.UserProfileResponse;
import com.swyp.invite.user.dto.UserProfileUpdateRequest;
import com.swyp.invite.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public UserProfileResponse getProfile(@AuthenticationPrincipal LoginUser loginUser) {
        return userService.getProfile(loginUser.userId());
    }

    @PutMapping
    public UserProfileResponse updateProfile(
            @AuthenticationPrincipal LoginUser loginUser, @Valid @RequestBody UserProfileUpdateRequest request) {
        return userService.updateProfile(loginUser.userId(), request);
    }
}
