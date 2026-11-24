package com.swyp.invite.global;

import com.swyp.invite.TestcontainersConfiguration;
import com.swyp.invite.event.entity.Event;
import com.swyp.invite.event.entity.EventType;
import com.swyp.invite.user.entity.User;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
public abstract class EntityTestSupport {

    @Autowired
    protected EntityManager em;

    protected <T> T persist(T entity) {
        em.persist(entity);
        return entity;
    }

    protected Event.EventBuilder memberEvent(User user, String shareCode) {
        return event(shareCode).hostUser(user);
    }

    protected Event.EventBuilder lightEvent(String shareCode) {
        return event(shareCode).hostCodeHash("hash-of-" + shareCode);
    }

    protected Event.EventBuilder event(String shareCode) {
        return Event.builder()
                .shareCode(shareCode)
                .eventType(EventType.BIRTHDAY)
                .title("생일 파티")
                .eventAt(LocalDateTime.of(2026, 12, 25, 19, 0))
                .place("우리 집");
    }

    protected Object raw(String sql, Long id) {
        return em.createNativeQuery(sql).setParameter(1, id).getSingleResult();
    }

    protected long count(String table) {
        return ((Number) em.createNativeQuery("select count(*) from " + table).getSingleResult()).longValue();
    }
}
