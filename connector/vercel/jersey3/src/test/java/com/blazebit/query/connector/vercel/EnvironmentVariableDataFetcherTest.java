/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.vercel;

import com.blazebit.query.QueryContext;
import com.blazebit.query.TypeReference;
import com.blazebit.query.impl.QueryContextBuilderImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class EnvironmentVariableDataFetcherTest {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new VercelSchemaProvider() );
		builder.registerSchemaObjectAlias( EnvironmentVariable.class, "VercelEnvVar" );
		CONTEXT = builder.build();
	}

	@Test
	void should_return_all_env_vars() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( EnvironmentVariable.class, List.of(
					VercelTestObjects.plainEnvVar(),
					VercelTestObjects.encryptedEnvVar(),
					VercelTestObjects.sensitiveEnvVar()
			) );

			var result = session.createQuery(
					"SELECT e.id, e.key, e.type, e.projectId FROM VercelEnvVar e",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}

	@Test
	void should_find_plain_text_env_vars() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( EnvironmentVariable.class, List.of(
					VercelTestObjects.plainEnvVar(),
					VercelTestObjects.encryptedEnvVar(),
					VercelTestObjects.sensitiveEnvVar()
			) );

			// Plain-text variables may expose secrets if misconfigured
			var result = session.createQuery(
					"SELECT e.id, e.key, e.projectId FROM VercelEnvVar e WHERE e.type = 'plain'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "key" ) ).isEqualTo( "APP_URL" );
		}
	}

	@Test
	void should_find_production_targeted_vars() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( EnvironmentVariable.class, List.of(
					VercelTestObjects.plainEnvVar(),
					VercelTestObjects.encryptedEnvVar(),
					VercelTestObjects.sensitiveEnvVar()
			) );

			// Variables scoped to exactly one target are often production-only secrets
			var result = session.createQuery(
					"SELECT e.id, e.key, e.type FROM VercelEnvVar e WHERE CARDINALITY(e.target) = 1",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_non_system_env_vars() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( EnvironmentVariable.class, List.of(
					VercelTestObjects.plainEnvVar(),
					VercelTestObjects.encryptedEnvVar(),
					VercelTestObjects.sensitiveEnvVar()
			) );

			// Vars without an explicit system flag are user-managed and may contain secrets
			var result = session.createQuery(
					"SELECT e.id, e.key FROM VercelEnvVar e WHERE e.systemGenerated IS NULL",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}
}
