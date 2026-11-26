package com.swyp.invite.rsvp.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Attendance {
    ATTENDING,
    DECLINED;

    @JsonCreator
    public static Attendance from(String value) {
        return Attendance.valueOf(value);
    }
}
