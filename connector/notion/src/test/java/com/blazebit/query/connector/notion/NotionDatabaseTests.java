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

public class NotionDatabaseTests {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new NotionSchemaProvider() );
		builder.registerSchemaObjectAlias( NotionDatabase.class, "NotionDatabase" );
		CONTEXT = builder.build();
	}

	private static NotionDatabase activeDatabase() {
		return new NotionDatabase(
				"db-001", "Employee Directory", "2023-01-15T10:00:00.000Z", "2024-03-01T09:00:00.000Z",
				"user-001", "user-002", false, false,
				"page_id", "page-100", "https://www.notion.so/db-001", false );
	}

	private static NotionDatabase inlineDatabase() {
		return new NotionDatabase(
				"db-002", "Sprint Tracker", "2024-01-01T08:00:00.000Z", "2024-04-01T12:00:00.000Z",
				"user-002", "user-002", false, false,
				"page_id", "page-200", "https://www.notion.so/db-002", true );
	}

	private static NotionDatabase archivedDatabase() {
		return new NotionDatabase(
				"db-003", "Old Vendor List", "2021-06-01T00:00:00.000Z", "2022-12-01T00:00:00.000Z",
				"user-003", "user-003", true, false,
				"workspace", null, "https://www.notion.so/db-003", false );
	}

	@Test
	void should_return_all_databases() {
		try (var session = CONTEXT.createSession()) {
			session.put( NotionDatabase.class, List.of( activeDatabase(), inlineDatabase(), archivedDatabase() ) );

			var result = session.createQuery(
					"SELECT d.id, d.title, d.parentType FROM NotionDatabase d",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}

	@Test
	void should_find_inline_databases() {
		try (var session = CONTEXT.createSession()) {
			session.put( NotionDatabase.class, List.of( activeDatabase(), inlineDatabase(), archivedDatabase() ) );

			var result = session.createQuery(
					"SELECT d.id, d.title FROM NotionDatabase d WHERE d.inline = true",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "title" ) ).isEqualTo( "Sprint Tracker" );
		}
	}

	@Test
	void should_find_archived_databases() {
		try (var session = CONTEXT.createSession()) {
			session.put( NotionDatabase.class, List.of( activeDatabase(), inlineDatabase(), archivedDatabase() ) );

			var result = session.createQuery(
					"SELECT d.id, d.title FROM NotionDatabase d WHERE d.archived = true",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "title" ) ).isEqualTo( "Old Vendor List" );
		}
	}

	@Test
	void should_find_active_databases() {
		try (var session = CONTEXT.createSession()) {
			session.put( NotionDatabase.class, List.of( activeDatabase(), inlineDatabase(), archivedDatabase() ) );

			var result = session.createQuery(
					"SELECT d.id, d.title FROM NotionDatabase d WHERE d.archived = false AND d.inTrash = false",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}
}
