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
 * Tests for Scaleway IAM API key queries — expiry, ownership, and credential hygiene.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public class ScalewayIamApiKeyTests {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new ScalewaySchemaProvider() );
		builder.registerSchemaObjectAlias( ScalewayIamApiKey.class, "ScalewayIamApiKey" );
		CONTEXT = builder.build();
	}

	private static ScalewayIamApiKey userKeyNoExpiry() {
		return new ScalewayIamApiKey(
				"SCWFOO123456789A",
				null,
				"usr-001",
				"CI pipeline key",
				true,
				"2024-01-15T10:00:00Z",
				null,
				"proj-xyz"
		);
	}

	private static ScalewayIamApiKey userKeyWithExpiry() {
		return new ScalewayIamApiKey(
				"SCWBAR987654321B",
				null,
				"usr-002",
				"Temp deployment key",
				true,
				"2025-06-01T10:00:00Z",
				"2026-06-01T10:00:00Z",
				"proj-xyz"
		);
	}

	private static ScalewayIamApiKey applicationKey() {
		return new ScalewayIamApiKey(
				"SCWAPP111222333C",
				"app-001",
				null,
				"Terraform service account key",
				false,
				"2023-12-01T10:00:00Z",
				null,
				"proj-abc"
		);
	}

	@Test
	void should_return_all_api_keys() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayIamApiKey.class, List.of( userKeyNoExpiry(), userKeyWithExpiry(), applicationKey() ) );

			var result = session.createQuery(
					"SELECT k.accessKey, k.userId, k.applicationId, k.expiresAt FROM ScalewayIamApiKey k",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}

	@Test
	void should_find_keys_without_expiry() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayIamApiKey.class, List.of( userKeyNoExpiry(), userKeyWithExpiry(), applicationKey() ) );

			var result = session.createQuery(
					"SELECT k.accessKey, k.userId, k.applicationId FROM ScalewayIamApiKey k WHERE k.expiresAt IS NULL",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_user_owned_keys() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayIamApiKey.class, List.of( userKeyNoExpiry(), userKeyWithExpiry(), applicationKey() ) );

			var result = session.createQuery(
					"SELECT k.accessKey, k.expiresAt FROM ScalewayIamApiKey k WHERE k.userId IS NOT NULL",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_application_owned_keys() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayIamApiKey.class, List.of( userKeyNoExpiry(), userKeyWithExpiry(), applicationKey() ) );

			var result = session.createQuery(
					"SELECT k.accessKey, k.applicationId FROM ScalewayIamApiKey k WHERE k.applicationId IS NOT NULL",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "accessKey" ) ).isEqualTo( "SCWAPP111222333C" );
		}
	}

	@Test
	void should_find_non_editable_keys() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayIamApiKey.class, List.of( userKeyNoExpiry(), userKeyWithExpiry(), applicationKey() ) );

			var result = session.createQuery(
					"SELECT k.accessKey FROM ScalewayIamApiKey k WHERE k.editable = false",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "accessKey" ) ).isEqualTo( "SCWAPP111222333C" );
		}
	}
}
