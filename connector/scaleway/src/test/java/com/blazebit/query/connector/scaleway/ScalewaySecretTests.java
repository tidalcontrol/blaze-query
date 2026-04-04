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
 * Tests for Scaleway Secret Manager queries — version tracking and secret hygiene.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public class ScalewaySecretTests {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new ScalewaySchemaProvider() );
		builder.registerSchemaObjectAlias( ScalewaySecret.class, "ScalewaySecret" );
		builder.registerSchemaObjectAlias( ScalewaySecretVersion.class, "ScalewaySecretVersion" );
		CONTEXT = builder.build();
	}

	private static ScalewaySecret activeSecret() {
		return new ScalewaySecret(
				"sec-001",
				"db-password",
				"Production database password",
				"ready",
				"fr-par",
				"proj-xyz",
				"org-abc",
				3,
				List.of( "env:prod", "team:infra" ),
				"2024-01-01T10:00:00Z",
				"2026-03-01T10:00:00Z"
		);
	}

	private static ScalewaySecret lockedSecret() {
		return new ScalewaySecret(
				"sec-002",
				"legacy-api-token",
				"Old integration token",
				"locked",
				"nl-ams",
				"proj-xyz",
				"org-abc",
				1,
				List.of( "env:prod" ),
				"2022-06-01T10:00:00Z",
				"2023-01-01T10:00:00Z"
		);
	}

	private static ScalewaySecret secretNoVersions() {
		return new ScalewaySecret(
				"sec-003",
				"orphaned-secret",
				"Created but never populated",
				"ready",
				"fr-par",
				"proj-dev",
				"org-abc",
				0,
				List.of(),
				"2025-12-01T10:00:00Z",
				"2025-12-01T10:00:00Z"
		);
	}

	private static ScalewaySecretVersion latestVersion() {
		return new ScalewaySecretVersion( "sec-001", "3", "enabled", "fr-par", "2026-03-01T10:00:00Z", "2026-03-01T10:00:00Z" );
	}

	private static ScalewaySecretVersion oldVersion() {
		return new ScalewaySecretVersion( "sec-001", "1", "disabled", "fr-par", "2024-01-01T10:00:00Z", "2024-06-01T10:00:00Z" );
	}

	@Test
	void should_return_all_secrets() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewaySecret.class, List.of( activeSecret(), lockedSecret(), secretNoVersions() ) );

			var result = session.createQuery(
					"SELECT s.id, s.name, s.status, s.versionCount FROM ScalewaySecret s",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}

	@Test
	void should_find_locked_secrets() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewaySecret.class, List.of( activeSecret(), lockedSecret(), secretNoVersions() ) );

			var result = session.createQuery(
					"SELECT s.id, s.name FROM ScalewaySecret s WHERE s.status = 'locked'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "legacy-api-token" );
		}
	}

	@Test
	void should_find_secrets_with_no_versions() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewaySecret.class, List.of( activeSecret(), lockedSecret(), secretNoVersions() ) );

			var result = session.createQuery(
					"SELECT s.id, s.name, s.projectId FROM ScalewaySecret s WHERE s.versionCount = 0",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "orphaned-secret" );
		}
	}

	@Test
	void should_find_active_secret_versions() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewaySecretVersion.class, List.of( latestVersion(), oldVersion() ) );

			var result = session.createQuery(
					"SELECT v.secretId, v.revision, v.status FROM ScalewaySecretVersion v WHERE v.status = 'enabled'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "revision" ) ).isEqualTo( "3" );
		}
	}

	@Test
	void should_count_secrets_by_project() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewaySecret.class, List.of( activeSecret(), lockedSecret(), secretNoVersions() ) );

			var result = session.createQuery(
					"SELECT s.projectId, COUNT(*) AS cnt FROM ScalewaySecret s GROUP BY s.projectId",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}
}
