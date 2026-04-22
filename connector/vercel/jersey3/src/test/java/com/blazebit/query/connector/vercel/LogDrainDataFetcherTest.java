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

public class LogDrainDataFetcherTest {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new VercelSchemaProvider() );
		builder.registerSchemaObjectAlias( LogDrain.class, "VercelLogDrain" );
		CONTEXT = builder.build();
	}

	@Test
	void should_return_all_log_drains() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( LogDrain.class, List.of(
					VercelTestObjects.siemLogDrain(),
					VercelTestObjects.buildOnlyLogDrain()
			) );

			var result = session.createQuery(
					"SELECT d.id, d.name, d.url FROM VercelLogDrain d",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_drains_missing_request_source() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( LogDrain.class, List.of(
					VercelTestObjects.siemLogDrain(),
					VercelTestObjects.buildOnlyLogDrain()
			) );

			// Drains with only one source type are likely missing HTTP request logs
			var result = session.createQuery(
					"SELECT d.id, d.name FROM VercelLogDrain d WHERE CARDINALITY(d.sources) = 1",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "Build Logs Only" );
		}
	}

	@Test
	void should_find_team_wide_drains() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( LogDrain.class, List.of(
					VercelTestObjects.siemLogDrain(),
					VercelTestObjects.buildOnlyLogDrain()
			) );

			var result = session.createQuery(
					"SELECT d.id, d.name FROM VercelLogDrain d WHERE CARDINALITY(d.projectIds) = 0",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "SIEM Drain" );
		}
	}
}
