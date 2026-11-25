package com.swyp.invite.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.swyp.invite.global.EntityTestSupport;
import com.swyp.invite.global.error.ApiException;
import com.swyp.invite.global.error.ErrorCode;
import com.swyp.invite.user.dto.UserProfileResponse;
import com.swyp.invite.user.dto.UserProfileUpdateRequest;
import com.swyp.invite.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserServiceTest extends EntityTestSupport {

    @Autowired
    private UserService service;

    @Test
    void 조회는_저장된_이름과_이메일과_정산정보를_원문으로_준다() {
        User user = new User("1001", "모임장");
        user.updateProfile("모임장", "user1@example.com", "가짜은행 000-000-000000");
        persist(user);
        em.flush();
        em.clear();

        UserProfileResponse profile = service.getProfile(user.getId());

        assertThat(profile).isEqualTo(new UserProfileResponse("모임장", "user1@example.com", "가짜은행 000-000-000000"));
    }

    @Test
    void 수정하면_세_값이_바뀌고_이메일과_정산정보는_DB에_원문으로_남지_않는다() {
        User user = persist(new User("1001", "모임장"));
        em.flush();

        UserProfileResponse profile = service.updateProfile(
                user.getId(), new UserProfileUpdateRequest("새 이름", "user1@example.com", "가짜은행 000-000-000000"));
        em.flush();
        em.clear();

        assertThat(profile).isEqualTo(new UserProfileResponse("새 이름", "user1@example.com", "가짜은행 000-000-000000"));
        assertThat(raw("select nickname from users where id = ?", user.getId())).isEqualTo("새 이름");
        assertThat(raw("select email from users where id = ?", user.getId()))
                .isNotNull()
                .isNotEqualTo("user1@example.com");
        assertThat(raw("select settlement_info from users where id = ?", user.getId()))
                .isNotNull()
                .isNotEqualTo("가짜은행 000-000-000000");
    }

    @Test
    void 이메일과_정산정보를_null로_수정하면_DB의_값이_비워진다() {
        User user = new User("1001", "모임장");
        user.updateProfile("모임장", "user1@example.com", "가짜은행 000-000-000000");
        persist(user);
        em.flush();

        service.updateProfile(user.getId(), new UserProfileUpdateRequest("모임장", null, null));
        em.flush();
        em.clear();

        assertThat(raw("select email from users where id = ?", user.getId())).isNull();
        assertThat(raw("select settlement_info from users where id = ?", user.getId()))
                .isNull();
    }

    @Test
    void 없는_회원을_조회하거나_수정하면_USER_NOT_FOUND다() {
        assertThatThrownBy(() -> service.getProfile(999_999L))
                .isInstanceOfSatisfying(
                        ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
        assertThatThrownBy(() -> service.updateProfile(999_999L, new UserProfileUpdateRequest("이름", null, null)))
                .isInstanceOfSatisfying(
                        ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
    }
}
