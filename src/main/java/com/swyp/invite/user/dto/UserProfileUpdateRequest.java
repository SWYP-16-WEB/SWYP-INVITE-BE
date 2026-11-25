package com.swyp.invite.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
        @NotBlank(message = "이름을 입력해 주세요") @Size(max = 30, message = "이름은 30자 이하로 입력해 주세요")
        String nickname,

        @Email(message = "이메일 형식이 올바르지 않습니다") @Size(max = 80, message = "이메일은 80자 이하로 입력해 주세요")
        String email,

        @Size(max = 40, message = "정산정보는 40자 이하로 입력해 주세요") String settlementInfo) {

    public UserProfileUpdateRequest {
        nickname = clean(nickname);
        email = clean(email);
        settlementInfo = clean(settlementInfo);
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
