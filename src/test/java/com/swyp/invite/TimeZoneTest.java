package com.swyp.invite;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class TimeZoneTest {

	@Test
	void 테스트의_기본_시간대는_KST다() {
		assertThat(ZoneId.systemDefault()).isEqualTo(ZoneId.of("Asia/Seoul"));
	}

}
