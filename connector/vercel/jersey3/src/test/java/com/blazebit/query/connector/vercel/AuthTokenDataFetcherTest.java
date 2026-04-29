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

public class AuthTokenDataFetcherTest {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new VercelSchemaProvider() );
		builder.registerSchemaObjectAlias( AuthToken.class, "VercelAuthToken" );
		CONTEXT = builder.build();
	}

	@Test
	void should_return_all_tokens() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( AuthToken.class, List.of(
					VercelTestObjects.activeToken(),
					VercelTestObjects.expiringToken(),
					VercelTestObjects.neverUsedToken()
			) );

			var result = session.createQuery(
					"SELECT t.id, t.name, t.type, t.origin FROM VercelAuthToken t",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}

	@Test
	void should_find_tokens_with_expiry() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( AuthToken.class, List.of(
					VercelTestObjects.activeToken(),
					VercelTestObjects.expiringToken(),
					VercelTestObjects.neverUsedToken()
			) );

			var result = session.createQuery(
					"SELECT t.id, t.name, t.expiresAt FROM VercelAuthToken t WHERE t.expiresAt IS NOT NULL",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "Short-lived token" );
		}
	}

	@Test
	void should_find_never_used_tokens() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( AuthToken.class, List.of(
					VercelTestObjects.activeToken(),
					VercelTestObjects.expiringToken(),
					VercelTestObjects.neverUsedToken()
			) );

			var result = session.createQuery(
					"SELECT t.id, t.name, t.createdAt FROM VercelAuthToken t WHERE t.activeAt IS NULL",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "Unused legacy token" );
		}
	}

	@Test
	void should_find_tokens_by_origin() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( AuthToken.class, List.of(
					VercelTestObjects.activeToken(),
					VercelTestObjects.expiringToken(),
					VercelTestObjects.neverUsedToken()
			) );

			var result = session.createQuery(
					"SELECT t.id, t.name FROM VercelAuthToken t WHERE t.origin = 'manual'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}
}
