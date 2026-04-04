/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.notion;

import com.blazebit.query.QueryContext;
import com.blazebit.query.TypeReference;
import com.blazebit.query.impl.QueryContextBuilderImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class NotionPageTests {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new NotionSchemaProvider() );
		builder.registerSchemaObjectAlias( NotionPage.class, "NotionPage" );
		CONTEXT = builder.build();
	}

	private static NotionPage activePage() {
		return new NotionPage(
				"page-001", "2024-01-10T09:00:00.000Z", "2024-03-15T14:30:00.000Z",
				"user-001", "user-002", false, false, false, null,
				"workspace", null, "https://www.notion.so/page-001" );
	}

	private static NotionPage publicPage() {
		return new NotionPage(
				"page-002", "2024-02-01T08:00:00.000Z", "2024-02-01T08:00:00.000Z",
				"user-001", "user-001", false, false, false,
				"https://myworkspace.notion.site/page-002",
				"workspace", null, "https://www.notion.so/page-002" );
	}

	private static NotionPage lockedPage() {
		return new NotionPage(
				"page-003", "2023-06-01T10:00:00.000Z", "2024-01-20T11:00:00.000Z",
				"user-003", "user-003", false, false, true, null,
				"database_id", "db-001", "https://www.notion.so/page-003" );
	}

	private static NotionPage trashedPage() {
		return new NotionPage(
				"page-004", "2023-01-01T00:00:00.000Z", "2023-12-01T00:00:00.000Z",
				"user-002", "user-002", false, true, false, null,
				"workspace", null, "https://www.notion.so/page-004" );
	}

	@Test
	void should_return_all_pages() {
		try (var session = CONTEXT.createSession()) {
			session.put( NotionPage.class, List.of( activePage(), publicPage(), lockedPage(), trashedPage() ) );

			var result = session.createQuery(
					"SELECT p.id, p.createdTime, p.parentType FROM NotionPage p",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 4 );
		}
	}

	@Test
	void should_find_publicly_shared_pages() {
		try (var session = CONTEXT.createSession()) {
			session.put( NotionPage.class, List.of( activePage(), publicPage(), lockedPage(), trashedPage() ) );

			var result = session.createQuery(
					"SELECT p.id, p.publicUrl FROM NotionPage p WHERE p.publicUrl IS NOT NULL",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "publicUrl" ) ).asString().contains( "notion.site" );
		}
	}

	@Test
	void should_find_locked_pages() {
		try (var session = CONTEXT.createSession()) {
			session.put( NotionPage.class, List.of( activePage(), publicPage(), lockedPage(), trashedPage() ) );

			var result = session.createQuery(
					"SELECT p.id FROM NotionPage p WHERE p.locked = true",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "id" ) ).isEqualTo( "page-003" );
		}
	}

	@Test
	void should_find_trashed_pages() {
		try (var session = CONTEXT.createSession()) {
			session.put( NotionPage.class, List.of( activePage(), publicPage(), lockedPage(), trashedPage() ) );

			var result = session.createQuery(
					"SELECT p.id FROM NotionPage p WHERE p.inTrash = true",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "id" ) ).isEqualTo( "page-004" );
		}
	}

	@Test
	void should_find_top_level_workspace_pages() {
		try (var session = CONTEXT.createSession()) {
			session.put( NotionPage.class, List.of( activePage(), publicPage(), lockedPage(), trashedPage() ) );

			var result = session.createQuery(
					"SELECT p.id FROM NotionPage p WHERE p.parentType = 'workspace'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}
}
