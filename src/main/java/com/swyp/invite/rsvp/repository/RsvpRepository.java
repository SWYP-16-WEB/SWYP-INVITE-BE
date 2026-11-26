package com.swyp.invite.rsvp.repository;

import com.swyp.invite.rsvp.entity.Rsvp;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RsvpRepository extends JpaRepository<Rsvp, Long> {}
