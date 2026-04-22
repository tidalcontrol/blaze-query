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

public class NotionDatabaseRowTests {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new NotionSchemaProvider() );
		builder.registerSchemaObjectAlias( NotionDatabaseRow.class, "NotionDatabaseRow" );
		CONTEXT = builder.build();
	}

	private static NotionDatabaseRow activeRow() {
		return new NotionDatabaseRow(
				"row-001", "db-001",
				"2024-01-10T09:00:00.000Z", "2024-03-01T12:00:00.000Z",
				"user-001", "user-002",
				false, false,
				"Alice Example",
				"Name: Alice Example; Email: alice@example.com; Department: Engineering; Status: Active" );
	}

	private static NotionDatabaseRow rowWithPii() {
		return new NotionDatabaseRow(
				"row-002", "db-001",
				"2024-01-15T10:00:00.000Z", "2024-02-20T11:00:00.000Z",
				"user-001", "user-001",
				false, false,
				"Bob Contractor",
				"Name: Bob Contractor; Email: bob@contractor.io; SSN: 123-45-6789; Status: Contractor" );
	}

	private static NotionDatabaseRow archivedRow() {
		return new NotionDatabaseRow(
				"row-003", "db-001",
				"2022-06-01T00:00:00.000Z", "2023-01-01T00:00:00.000Z",
				"user-003", "user-003",
				true, false,
				"Former Employee",
				"Name: Former Employee; Email: former@example.com; Status: Offboarded" );
	}

	private static NotionDatabaseRow trashedRow() {
		return new NotionDatabaseRow(
				"row-004", "db-002",
				"2023-01-01T00:00:00.000Z", "2023-12-01T00:00:00.000Z",
				"user-002", "user-002",
				false, true,
				"Deleted Record",
				"Name: Deleted Record; Notes: password123" );
	}

	@Test
	void should_return_all_rows() {
		try (var session = CONTEXT.createSession()) {
			session.put( NotionDatabaseRow.class, List.of( activeRow(), rowWithPii(), archivedRow(), trashedRow() ) );

			var result = session.createQuery(
					"SELECT r.id, r.title, r.databaseId FROM NotionDatabaseRow r",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 4 );
		}
	}

	@Test
	void should_find_active_rows() {
		try (var session = CONTEXT.createSession()) {
			session.put( NotionDatabaseRow.class, List.of( activeRow(), rowWithPii(), archivedRow(), trashedRow() ) );

			var result = session.createQuery(
					"SELECT r.id, r.title FROM NotionDatabaseRow r WHERE r.archived = false AND r.inTrash = false",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_scan_properties_for_sensitive_data() {
		try (var session = CONTEXT.createSession()) {
			session.put( NotionDatabaseRow.class, List.of( activeRow(), rowWithPii(), archivedRow(), trashedRow() ) );

			var result = session.createQuery(
					"SELECT r.id, r.title, r.databaseId FROM NotionDatabaseRow r WHERE r.propertiesPlainText LIKE '%SSN%' OR r.propertiesPlainText LIKE '%password%'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_rows_in_trash() {
		try (var session = CONTEXT.createSession()) {
			session.put( NotionDatabaseRow.class, List.of( activeRow(), rowWithPii(), archivedRow(), trashedRow() ) );

			var result = session.createQuery(
					"SELECT r.id, r.title FROM NotionDatabaseRow r WHERE r.inTrash = true",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "title" ) ).isEqualTo( "Deleted Record" );
		}
	}

	@Test
	void should_count_rows_per_database() {
		try (var session = CONTEXT.createSession()) {
			session.put( NotionDatabaseRow.class, List.of( activeRow(), rowWithPii(), archivedRow(), trashedRow() ) );

			var result = session.createQuery(
					"SELECT r.databaseId, COUNT(*) AS row_count FROM NotionDatabaseRow r GROUP BY r.databaseId",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}
}
