package com.swyp.invite.checklist.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.swyp.invite.event.entity.Event;
import com.swyp.invite.global.EntityTestSupport;
import org.junit.jupiter.api.Test;

class ChecklistItemTest extends EntityTestSupport {

    @Test
    void 저장하고_다시_읽으면_값이_같다() {
        Event event = persist(lightEvent("share-1").build());
        ChecklistItem item = persist(new ChecklistItem(event, "케이크", "초는 따로 사기", "https://example.com/cake"));
        em.flush();
        em.clear();

        ChecklistItem found = em.find(ChecklistItem.class, item.getId());
        assertThat(found.getContent()).isEqualTo("케이크");
        assertThat(found.getMemo()).isEqualTo("초는 따로 사기");
        assertThat(found.getPurchaseUrl()).isEqualTo("https://example.com/cake");
        assertThat(found.isDone()).isFalse();
    }

    @Test
    void 메모와_구매링크는_비워_둘_수_있다() {
        Event event = persist(lightEvent("share-1").build());
        ChecklistItem item = persist(new ChecklistItem(event, "케이크", null, null));
        em.flush();
        em.clear();

        ChecklistItem found = em.find(ChecklistItem.class, item.getId());
        assertThat(found.getMemo()).isNull();
        assertThat(found.getPurchaseUrl()).isNull();
    }

    @Test
    void 구매링크_2000자가_들어간다() {
        String longest = "https://example.com/" + "a".repeat(1980);
        Event event = persist(lightEvent("share-1").build());
        ChecklistItem item = persist(new ChecklistItem(event, "케이크", null, longest));
        em.flush();
        em.clear();

        assertThat(em.find(ChecklistItem.class, item.getId()).getPurchaseUrl())
                .hasSize(2000)
                .isEqualTo(longest);
    }
}
