package com.swyp.invite.event.repository;

import com.swyp.invite.event.entity.Event;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findByShareCode(String shareCode);
}
