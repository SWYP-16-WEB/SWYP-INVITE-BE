package com.swyp.invite;

import org.springframework.boot.SpringApplication;

public class TestInviteApplication {

	public static void main(String[] args) {
		SpringApplication.from(InviteApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
