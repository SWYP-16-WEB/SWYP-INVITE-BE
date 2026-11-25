package com.swyp.invite.user.service;

import com.swyp.invite.global.error.ApiException;
import com.swyp.invite.global.error.ErrorCode;
import com.swyp.invite.user.entity.User;
import com.swyp.invite.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private static final int NICKNAME_MAX_LENGTH = 30;

    private final UserRepository userRepository;

    @Transactional
    public Long findOrRegister(String kakaoId, String kakaoNickname) {
        return userRepository
                .findByKakaoId(kakaoId)
                .orElseGet(() -> userRepository.save(new User(kakaoId, nickname(kakaoNickname))))
                .getId();
    }

    private String nickname(String kakaoNickname) {
        if (kakaoNickname == null || kakaoNickname.isBlank()) {
            throw new ApiException(ErrorCode.KAKAO_AUTH_FAILED);
        }
        String stripped = kakaoNickname.strip();
        int length = Math.min(NICKNAME_MAX_LENGTH, stripped.codePointCount(0, stripped.length()));
        return stripped.substring(0, stripped.offsetByCodePoints(0, length));
    }
}
