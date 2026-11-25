package com.swyp.invite.user.service;

import com.swyp.invite.global.error.ApiException;
import com.swyp.invite.global.error.ErrorCode;
import com.swyp.invite.user.dto.UserProfileResponse;
import com.swyp.invite.user.dto.UserProfileUpdateRequest;
import com.swyp.invite.user.entity.User;
import com.swyp.invite.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        return UserProfileResponse.from(find(userId));
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UserProfileUpdateRequest request) {
        User user = find(userId);
        user.updateProfile(request.nickname(), request.email(), request.settlementInfo());
        return UserProfileResponse.from(user);
    }

    private User find(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }
}
