/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.blazebit.query.QueryContext;
import com.blazebit.query.TypeReference;
import com.blazebit.query.impl.QueryContextBuilderImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Scaleway Managed Database queries — public endpoints, HA, and backup retention.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public class ScalewayDatabaseTests {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new ScalewaySchemaProvider() );
		builder.registerSchemaObjectAlias( ScalewayDatabase.class, "ScalewayDatabase" );
		CONTEXT = builder.build();
	}

	private static ScalewayDatabase productionDb() {
		return new ScalewayDatabase(
				"rdb-001",
				"prod-postgres",
				"ready",
				"PostgreSQL-15",
				"bssd",
				"fr-par",
				"proj-xyz",
				"org-abc",
				true,
				false,
				7,
				"10.0.0.5",
				List.of( "env:prod" ),
				"2024-01-01T00:00:00Z",
				"2026-01-01T00:00:00Z"
		);
	}

	private static ScalewayDatabase devDb() {
		return new ScalewayDatabase(
				"rdb-002",
				"dev-mysql",
				"ready",
				"MySQL-8",
				"lssd",
				"nl-ams",
				"proj-xyz",
				"org-abc",
				false,
				true,
				1,
				"51.158.100.20",
				List.of( "env:dev" ),
				"2024-06-01T00:00:00Z",
				"2026-02-01T00:00:00Z"
		);
	}

	private static ScalewayDatabase legacyDb() {
		return new ScalewayDatabase(
				"rdb-003",
				"legacy-postgres",
				"ready",
				"PostgreSQL-11",
				"lssd",
				"fr-par",
				"proj-xyz",
				"org-abc",
				false,
				false,
				3,
				"10.0.0.10",
				List.of( "deprecated" ),
				"2022-01-01T00:00:00Z",
				"2024-01-01T00:00:00Z"
		);
	}

	@Test
	void should_return_all_databases() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayDatabase.class, List.of( productionDb(), devDb(), legacyDb() ) );

			var result = session.createQuery(
					"SELECT d.id, d.name, d.engine, d.status FROM ScalewayDatabase d",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}

	@Test
	void should_find_publicly_accessible_databases() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayDatabase.class, List.of( productionDb(), devDb(), legacyDb() ) );

			var result = session.createQuery(
					"SELECT d.id, d.name FROM ScalewayDatabase d WHERE d.publiclyAccessible = true",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "dev-mysql" );
		}
	}

	@Test
	void should_find_databases_without_ha() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayDatabase.class, List.of( productionDb(), devDb(), legacyDb() ) );

			var result = session.createQuery(
					"SELECT d.id, d.name FROM ScalewayDatabase d WHERE d.haEnabled = false AND d.status = 'ready'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_databases_with_short_backup_retention() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayDatabase.class, List.of( productionDb(), devDb(), legacyDb() ) );

			var result = session.createQuery(
					"SELECT d.id, d.name, d.backupRetentionDays FROM ScalewayDatabase d WHERE d.backupRetentionDays < 7",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_legacy_engine_databases() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayDatabase.class, List.of( productionDb(), devDb(), legacyDb() ) );

			var result = session.createQuery(
					"SELECT d.id, d.name, d.engine FROM ScalewayDatabase d WHERE d.engine LIKE '%11%' OR d.engine LIKE '%8%'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}
}
