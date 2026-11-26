package com.swyp.invite.event.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.swyp.invite.event.entity.Event;
import com.swyp.invite.event.entity.EventType;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EventTimePolicyTest {

    private static final LocalDateTime EVENT_AT = LocalDateTime.of(2027, 1, 16, 18, 30);

    @ParameterizedTest
    @CsvSource({
        "2027-01-18T18:29:59.999999999, false",
        "2027-01-18T18:30:00, true",
        "2027-01-18T18:30:00.000000001, true"
    })
    void 시작_시각에서_정확히_48시간부터_만료된다(LocalDateTime now, boolean expired) {
        EventTimePolicy.Status status = policy(now).evaluate(event(null));

        assertThat(status.expired()).isEqualTo(expired);
        assertThat(status.rsvpOpen()).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
        ", 2027-01-16T23:59:59.999999999, true",
        ", 2027-01-17T00:00:00, false",
        "2027-01-10, 2027-01-10T23:59:59.999999999, true",
        "2027-01-10, 2027-01-11T00:00:00, false",
        "2027-01-16, 2027-01-16T23:59:59.999999999, true",
        "2027-01-16, 2027-01-17T00:00:00, false",
        "2027-01-20, 2027-01-16T23:59:59.999999999, true",
        "2027-01-20, 2027-01-17T00:00:00, false"
    })
    void 마감일과_행사일_중_빠른_날짜의_다음날_자정부터_응답을_닫는다(LocalDate deadline, LocalDateTime now, boolean rsvpOpen) {
        EventTimePolicy.Status status = policy(now).evaluate(event(deadline));

        assertThat(status.expired()).isFalse();
        assertThat(status.rsvpOpen()).isEqualTo(rsvpOpen);
    }

    @Test
    void Clock의_시간대가_UTC여도_KST의_자정으로_마감한다() {
        EventTimePolicy policy =
                new EventTimePolicy(Clock.fixed(Instant.parse("2027-01-10T15:00:00Z"), ZoneOffset.UTC));

        assertThat(policy.evaluate(event(LocalDate.of(2027, 1, 10))).rsvpOpen()).isFalse();
    }

    private EventTimePolicy policy(LocalDateTime now) {
        return new EventTimePolicy(Clock.fixed(now.toInstant(ZoneOffset.ofHours(9)), ZoneOffset.UTC));
    }

    private Event event(LocalDate deadline) {
        return Event.builder()
                .eventType(EventType.BIRTHDAY)
                .eventAt(EVENT_AT)
                .rsvpDeadline(deadline)
                .build();
    }
}
