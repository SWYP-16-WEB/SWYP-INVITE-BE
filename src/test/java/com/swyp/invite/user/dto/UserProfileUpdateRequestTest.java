package com.swyp.invite.user.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserProfileUpdateRequestTest {

    @Test
    void 앞뒤_공백을_뗀다() {
        UserProfileUpdateRequest request =
                new UserProfileUpdateRequest("  모임장  ", " user1@example.com ", " 가짜은행 000-000-000000 ");

        assertThat(request.nickname()).isEqualTo("모임장");
        assertThat(request.email()).isEqualTo("user1@example.com");
        assertThat(request.settlementInfo()).isEqualTo("가짜은행 000-000-000000");
    }

    @Test
    void 빈_문자열과_공백뿐인_값은_null이_된다() {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest("   ", "", "  ");

        assertThat(request.nickname()).isNull();
        assertThat(request.email()).isNull();
        assertThat(request.settlementInfo()).isNull();
    }

    @Test
    void null은_null로_둔다() {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest("모임장", null, null);

        assertThat(request.email()).isNull();
        assertThat(request.settlementInfo()).isNull();
    }
}
