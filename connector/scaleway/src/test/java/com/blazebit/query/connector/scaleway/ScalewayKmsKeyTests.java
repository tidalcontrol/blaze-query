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
 * Tests for Scaleway KMS key queries — rotation policy, key state, and compliance.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public class ScalewayKmsKeyTests {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new ScalewaySchemaProvider() );
		builder.registerSchemaObjectAlias( ScalewayKmsKey.class, "ScalewayKmsKey" );
		CONTEXT = builder.build();
	}

	private static ScalewayKmsKey enabledKeyWithRotation() {
		return new ScalewayKmsKey(
				"key-001",
				"prod-data-key",
				"Encrypts production database",
				"enabled",
				"aes_256_gcm",
				"fr-par",
				"proj-xyz",
				"org-abc",
				"P90D",
				"2026-07-01T00:00:00Z",
				"2026-04-01T00:00:00Z",
				true,
				"2024-01-15T10:00:00Z",
				"2026-04-01T10:00:00Z"
		);
	}

	private static ScalewayKmsKey enabledKeyNoRotation() {
		return new ScalewayKmsKey(
				"key-002",
				"backup-key",
				"Encrypts backup data",
				"enabled",
				"aes_256_gcm",
				"nl-ams",
				"proj-xyz",
				"org-abc",
				null,
				null,
				null,
				false,
				"2023-06-01T10:00:00Z",
				"2024-01-01T10:00:00Z"
		);
	}

	private static ScalewayKmsKey disabledKey() {
		return new ScalewayKmsKey(
				"key-003",
				"deprecated-key",
				"Old key, no longer used",
				"disabled",
				"aes_256_gcm",
				"fr-par",
				"proj-old",
				"org-abc",
				null,
				null,
				null,
				false,
				"2022-01-01T10:00:00Z",
				"2024-01-01T10:00:00Z"
		);
	}

	@Test
	void should_return_all_kms_keys() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayKmsKey.class, List.of( enabledKeyWithRotation(), enabledKeyNoRotation(), disabledKey() ) );

			var result = session.createQuery(
					"SELECT k.id, k.name, k.state, k.rotationEnabled FROM ScalewayKmsKey k",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}

	@Test
	void should_find_keys_without_rotation_policy() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayKmsKey.class, List.of( enabledKeyWithRotation(), enabledKeyNoRotation(), disabledKey() ) );

			var result = session.createQuery(
					"SELECT k.id, k.name, k.state FROM ScalewayKmsKey k WHERE k.rotationEnabled = false AND k.state = 'enabled'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "backup-key" );
		}
	}

	@Test
	void should_find_disabled_keys() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayKmsKey.class, List.of( enabledKeyWithRotation(), enabledKeyNoRotation(), disabledKey() ) );

			var result = session.createQuery(
					"SELECT k.id, k.name FROM ScalewayKmsKey k WHERE k.state = 'disabled'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "deprecated-key" );
		}
	}

	@Test
	void should_find_keys_with_rotation_configured() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayKmsKey.class, List.of( enabledKeyWithRotation(), enabledKeyNoRotation(), disabledKey() ) );

			var result = session.createQuery(
					"SELECT k.id, k.name, k.rotationPeriod, k.nextRotationAt FROM ScalewayKmsKey k WHERE k.rotationEnabled = true",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "rotationPeriod" ) ).isEqualTo( "P90D" );
		}
	}

	@Test
	void should_count_keys_by_region() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayKmsKey.class, List.of( enabledKeyWithRotation(), enabledKeyNoRotation(), disabledKey() ) );

			var result = session.createQuery(
					"SELECT k.region, COUNT(*) AS cnt FROM ScalewayKmsKey k GROUP BY k.region",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}
}
