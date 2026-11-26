package com.swyp.invite.rsvp.dto;

import com.swyp.invite.rsvp.entity.Attendance;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RsvpCreateRequest(
        @NotBlank(message = "이름을 입력해 주세요") @Size(max = 20, message = "이름은 20자 이하로 입력해 주세요")
        String name,

        @NotNull(message = "참석 여부를 선택해 주세요") Attendance attendance,

        @Email(message = "이메일 형식이 올바르지 않습니다") @Size(max = 80, message = "이메일은 80자 이하로 입력해 주세요")
        String email) {

    public RsvpCreateRequest {
        name = clean(name);
        email = clean(email);
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
