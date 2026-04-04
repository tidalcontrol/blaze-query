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

public class DeploymentDataFetcherTest {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new VercelSchemaProvider() );
		builder.registerSchemaObjectAlias( Deployment.class, "VercelDeployment" );
		CONTEXT = builder.build();
	}

	@Test
	void should_return_all_deployments() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( Deployment.class, List.of(
					VercelTestObjects.gitDeployment(),
					VercelTestObjects.cliDeployment(),
					VercelTestObjects.failedDeployment()
			) );

			var result = session.createQuery(
					"SELECT d.uid, d.name, d.state, d.target, d.source FROM VercelDeployment d",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}

	@Test
	void should_find_non_git_production_deployments() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( Deployment.class, List.of(
					VercelTestObjects.gitDeployment(),
					VercelTestObjects.cliDeployment(),
					VercelTestObjects.failedDeployment()
			) );

			// Non-git production deployments bypass standard CI/CD review controls
			var result = session.createQuery(
					"SELECT d.uid, d.source, d.creator.email FROM VercelDeployment d WHERE d.target = 'production' AND d.source <> 'git'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "source" ) ).isEqualTo( "cli" );
		}
	}

	@Test
	void should_find_failed_deployments() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( Deployment.class, List.of(
					VercelTestObjects.gitDeployment(),
					VercelTestObjects.cliDeployment(),
					VercelTestObjects.failedDeployment()
			) );

			var result = session.createQuery(
					"SELECT d.uid, d.name, d.errorCode FROM VercelDeployment d WHERE d.state = 'ERROR'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
		}
	}

	@Test
	void should_find_deployments_with_failed_checks() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( Deployment.class, List.of(
					VercelTestObjects.gitDeployment(),
					VercelTestObjects.cliDeployment(),
					VercelTestObjects.failedDeployment()
			) );

			var result = session.createQuery(
					"SELECT d.uid, d.name FROM VercelDeployment d WHERE d.checksConclusion = 'failed'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
		}
	}
}
