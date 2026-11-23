package com.swyp.invite;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import jakarta.persistence.EntityManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SchemaTest {

	@Autowired
	EntityManager em;

	@Test
	void V1이_테이블_다섯_개를_만든다() {
		List<?> tables = em.createNativeQuery("""
				select table_name from information_schema.tables
				where table_schema = database() and table_name <> 'flyway_schema_history'
				order by table_name
				""").getResultList();

		assertThat(tables).hasToString("[checklist_items, events, reminders, rsvps, users]");
	}

	@Test
	void Flyway가_V1을_적용했다() {
		Object applied = em.createNativeQuery(
				"select count(*) from flyway_schema_history where version = '1' and success = 1")
				.getSingleResult();

		assertThat(applied).hasToString("1");
	}

}
