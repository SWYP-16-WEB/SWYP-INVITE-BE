package com.swyp.invite.rsvp.repository;

import com.swyp.invite.rsvp.entity.Rsvp;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RsvpRepository extends JpaRepository<Rsvp, Long> {

    List<Rsvp> findAllByEventIdOrderByCreatedAtDescIdDesc(Long eventId);
}
