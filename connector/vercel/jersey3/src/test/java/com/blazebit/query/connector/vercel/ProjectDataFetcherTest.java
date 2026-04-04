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

public class ProjectDataFetcherTest {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new VercelSchemaProvider() );
		builder.registerSchemaObjectAlias( Project.class, "VercelProject" );
		CONTEXT = builder.build();
	}

	@Test
	void should_return_all_projects() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( Project.class, List.of(
					VercelTestObjects.protectedProject(),
					VercelTestObjects.unprotectedProject()
			) );

			var result = session.createQuery(
					"SELECT p.id, p.name, p.framework FROM VercelProject p",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_projects_without_protection() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( Project.class, List.of(
					VercelTestObjects.protectedProject(),
					VercelTestObjects.unprotectedProject()
			) );

			// Projects with no password or SSO protection — all deployments are publicly accessible
			var result = session.createQuery(
					"""
					SELECT p.id, p.name FROM VercelProject p
					WHERE p.passwordProtection IS NULL AND p.ssoProtection IS NULL
					""",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "internal-tools" );
		}
	}

	@Test
	void should_find_projects_auto_exposing_system_envs() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( Project.class, List.of(
					VercelTestObjects.protectedProject(),
					VercelTestObjects.unprotectedProject()
			) );

			// Projects that automatically expose system environment variables may leak infra metadata
			var result = session.createQuery(
					"SELECT p.id, p.name FROM VercelProject p WHERE p.autoExposeSystemEnvs = true",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "internal-tools" );
		}
	}

	@Test
	void should_find_projects_with_password_protection() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( Project.class, List.of(
					VercelTestObjects.protectedProject(),
					VercelTestObjects.unprotectedProject()
			) );

			var result = session.createQuery(
					"SELECT p.id, p.name, p.passwordProtection.deploymentType FROM VercelProject p WHERE p.passwordProtection IS NOT NULL",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "customer-portal" );
		}
	}
}
