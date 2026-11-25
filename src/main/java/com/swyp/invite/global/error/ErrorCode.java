package com.swyp.invite.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    VALIDATION_FAILED(400, "입력값이 올바르지 않습니다"),
    UNAUTHENTICATED(401, "로그인이 필요합니다"),
    KAKAO_AUTH_FAILED(401, "카카오 인증에 실패했습니다"),
    HOST_CODE_INVALID(401, "관리 코드가 맞지 않습니다"),
    USER_NOT_FOUND(401, "회원을 찾을 수 없습니다"),
    FORBIDDEN(403, "본인이 만든 이벤트가 아닙니다"),
    CSRF_TOKEN_INVALID(403, "CSRF 토큰이 없거나 맞지 않습니다"),
    INVITATION_NOT_FOUND(404, "존재하지 않는 초대장입니다"),
    EVENT_NOT_FOUND(404, "존재하지 않는 이벤트입니다"),
    RSVP_CLOSED(409, "응답 기간이 끝났습니다"),
    EVENT_ENDED(409, "행사일이 지난 이벤트는 수정할 수 없습니다"),
    EVENT_EXPIRED(410, "만료된 초대장입니다"),
    FILE_TOO_LARGE(413, "파일이 너무 큽니다"),
    UNSUPPORTED_FILE_TYPE(415, "지원하지 않는 파일 형식입니다"),
    TOO_MANY_ATTEMPTS(429, "시도가 너무 많습니다. 잠시 뒤 다시 시도해 주세요"),
    AI_GENERATION_FAILED(502, "AI 체크리스트 생성에 실패했습니다");

    private final int status;

    private final String message;
}
