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

public class IntegrationConfigurationDataFetcherTest {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new VercelSchemaProvider() );
		builder.registerSchemaObjectAlias( IntegrationConfiguration.class, "VercelIntegration" );
		CONTEXT = builder.build();
	}

	@Test
	void should_return_all_integrations() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( IntegrationConfiguration.class, List.of(
					VercelTestObjects.activeIntegration(),
					VercelTestObjects.suspendedIntegration()
			) );

			var result = session.createQuery(
					"SELECT i.id, i.slug, i.status FROM VercelIntegration i",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_suspended_integrations() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( IntegrationConfiguration.class, List.of(
					VercelTestObjects.activeIntegration(),
					VercelTestObjects.suspendedIntegration()
			) );

			var result = session.createQuery(
					"SELECT i.id, i.slug FROM VercelIntegration i WHERE i.status = 'suspended'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "slug" ) ).isEqualTo( "legacy-connector" );
		}
	}

	@Test
	void should_find_team_wide_integrations() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( IntegrationConfiguration.class, List.of(
					VercelTestObjects.activeIntegration(),
					VercelTestObjects.suspendedIntegration()
			) );

			// Integrations with no project scoping have access to all projects
			var result = session.createQuery(
					"SELECT i.id, i.slug FROM VercelIntegration i WHERE CARDINALITY(i.projects) = 0",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "slug" ) ).isEqualTo( "datadog" );
		}
	}
}
