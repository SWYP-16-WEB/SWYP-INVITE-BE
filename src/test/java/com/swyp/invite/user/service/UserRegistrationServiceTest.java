package com.swyp.invite.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.swyp.invite.global.EntityTestSupport;
import com.swyp.invite.global.error.ApiException;
import com.swyp.invite.global.error.ErrorCode;
import com.swyp.invite.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserRegistrationServiceTest extends EntityTestSupport {

    @Autowired
    private UserRegistrationService service;

    @Test
    void 처음_보는_카카오_회원번호면_회원을_만든다() {
        Long userId = service.findOrRegister("1001", "모임장");
        em.flush();
        em.clear();

        User found = em.find(User.class, userId);
        assertThat(found.getKakaoId()).isEqualTo("1001");
        assertThat(found.getNickname()).isEqualTo("모임장");
    }

    @Test
    void 이미_있는_카카오_회원번호면_같은_회원을_주고_이름을_바꾸지_않는다() {
        User user = persist(new User("1001", "고친 이름"));
        em.flush();

        Long userId = service.findOrRegister("1001", "카카오 닉네임");
        em.flush();
        em.clear();

        assertThat(userId).isEqualTo(user.getId());
        assertThat(em.find(User.class, userId).getNickname()).isEqualTo("고친 이름");
        assertThat(count("users")).isEqualTo(1);
    }

    @Test
    void 닉네임의_앞뒤_공백을_떼고_30글자까지만_넣는다() {
        Long userId = service.findOrRegister("1001", "  " + "가".repeat(31) + "  ");
        em.flush();
        em.clear();

        assertThat(em.find(User.class, userId).getNickname()).isEqualTo("가".repeat(30));
    }

    @Test
    void 이모지가_섞인_닉네임도_30글자에서_자른다() {
        Long userId = service.findOrRegister("1001", "😀".repeat(31));
        em.flush();
        em.clear();

        assertThat(em.find(User.class, userId).getNickname()).isEqualTo("😀".repeat(30));
    }

    @Test
    void 닉네임이_없으면_회원을_만들지_않는다() {
        assertThatThrownBy(() -> service.findOrRegister("1001", null))
                .isInstanceOfSatisfying(
                        ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.KAKAO_AUTH_FAILED));
        assertThatThrownBy(() -> service.findOrRegister("1002", "   "))
                .isInstanceOfSatisfying(
                        ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.KAKAO_AUTH_FAILED));

        assertThat(count("users")).isZero();
    }
}
