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
 * Tests for Scaleway Serverless queries — Containers and Functions.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public class ScalewayServerlessTests {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new ScalewaySchemaProvider() );
		builder.registerSchemaObjectAlias( ScalewayContainer.class, "ScalewayContainer" );
		builder.registerSchemaObjectAlias( ScalewayFunction.class, "ScalewayFunction" );
		CONTEXT = builder.build();
	}

	// -------------------------------------------------------------------------
	// Container test objects
	// -------------------------------------------------------------------------

	private static ScalewayContainer privateContainer() {
		return new ScalewayContainer(
				"con-001",
				"api-handler",
				"ns-001",
				"ready",
				"fr-par",
				"private",
				"h2c",
				1120,
				2048,
				1,
				5,
				true,
				"2025-01-01T00:00:00Z",
				"2026-01-01T00:00:00Z"
		);
	}

	private static ScalewayContainer publicContainer() {
		return new ScalewayContainer(
				"con-002",
				"public-webhook",
				"ns-001",
				"ready",
				"fr-par",
				"public",
				"http1",
				140,
				256,
				0,
				20,
				false,
				"2025-02-01T00:00:00Z",
				"2026-02-01T00:00:00Z"
		);
	}

	private static ScalewayContainer errorContainer() {
		return new ScalewayContainer(
				"con-003",
				"broken-service",
				"ns-001",
				"error",
				"fr-par",
				"private",
				"http1",
				140,
				256,
				0,
				10,
				false,
				"2025-03-01T00:00:00Z",
				"2026-03-01T00:00:00Z"
		);
	}

	@Test
	void should_return_all_containers() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayContainer.class, List.of( privateContainer(), publicContainer(), errorContainer() ) );

			var result = session.createQuery(
					"SELECT c.id, c.name, c.status, c.privacy FROM ScalewayContainer c",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}

	@Test
	void should_find_public_containers() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayContainer.class, List.of( privateContainer(), publicContainer(), errorContainer() ) );

			var result = session.createQuery(
					"SELECT c.id, c.name FROM ScalewayContainer c WHERE c.privacy = 'public'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "public-webhook" );
		}
	}

	@Test
	void should_find_containers_with_env_vars() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayContainer.class, List.of( privateContainer(), publicContainer(), errorContainer() ) );

			var result = session.createQuery(
					"SELECT c.id, c.name FROM ScalewayContainer c WHERE c.hasEnvVars = true",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
		}
	}

	// -------------------------------------------------------------------------
	// Function test objects
	// -------------------------------------------------------------------------

	private static ScalewayFunction privateFunction() {
		return new ScalewayFunction(
				"fn-001",
				"data-processor",
				"ns-002",
				"ready",
				"fr-par",
				"python311",
				"private",
				512,
				280,
				0,
				10,
				true,
				"2025-01-01T00:00:00Z",
				"2026-01-01T00:00:00Z"
		);
	}

	private static ScalewayFunction publicFunction() {
		return new ScalewayFunction(
				"fn-002",
				"public-api",
				"ns-002",
				"ready",
				"fr-par",
				"node20",
				"public",
				256,
				140,
				0,
				50,
				false,
				"2025-02-01T00:00:00Z",
				"2026-02-01T00:00:00Z"
		);
	}

	@Test
	void should_return_all_functions() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayFunction.class, List.of( privateFunction(), publicFunction() ) );

			var result = session.createQuery(
					"SELECT f.id, f.name, f.status, f.runtime FROM ScalewayFunction f",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_public_functions() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayFunction.class, List.of( privateFunction(), publicFunction() ) );

			var result = session.createQuery(
					"SELECT f.id, f.name FROM ScalewayFunction f WHERE f.privacy = 'public'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "public-api" );
		}
	}

	@Test
	void should_find_ready_functions_by_runtime() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayFunction.class, List.of( privateFunction(), publicFunction() ) );

			var result = session.createQuery(
					"SELECT f.id, f.name FROM ScalewayFunction f WHERE f.status = 'ready' AND f.runtime = 'python311'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
		}
	}
}
