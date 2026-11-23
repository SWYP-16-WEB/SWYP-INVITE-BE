package com.swyp.invite.global.error;

import java.util.List;

public record ApiErrorResponse(String code, String message, List<InvalidField> errors) {

	public record InvalidField(String field, String message) {
	}

}
