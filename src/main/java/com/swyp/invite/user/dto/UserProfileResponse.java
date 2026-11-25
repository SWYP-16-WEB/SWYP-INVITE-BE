package com.swyp.invite.user.dto;

import com.swyp.invite.user.entity.User;

public record UserProfileResponse(String nickname, String email, String settlementInfo) {

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(user.getNickname(), user.getEmail(), user.getSettlementInfo());
    }
}
