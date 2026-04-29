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

public class AccessGroupDataFetcherTest {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new VercelSchemaProvider() );
		builder.registerSchemaObjectAlias( AccessGroup.class, "VercelAccessGroup" );
		CONTEXT = builder.build();
	}

	@Test
	void should_return_all_access_groups() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( AccessGroup.class, List.of(
					VercelTestObjects.dsyncManagedGroup(),
					VercelTestObjects.manualGroup()
			) );

			var result = session.createQuery(
					"SELECT g.accessGroupId, g.name, g.membersCount FROM VercelAccessGroup g",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_groups_not_managed_by_dsync() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( AccessGroup.class, List.of(
					VercelTestObjects.dsyncManagedGroup(),
					VercelTestObjects.manualGroup()
			) );

			// Groups not managed by directory sync require manual member management — higher drift risk
			var result = session.createQuery(
					"SELECT g.accessGroupId, g.name FROM VercelAccessGroup g WHERE g.isDsyncManaged = false OR g.isDsyncManaged IS NULL",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "Frontend Devs" );
		}
	}

	@Test
	void should_find_groups_by_team() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( AccessGroup.class, List.of(
					VercelTestObjects.dsyncManagedGroup(),
					VercelTestObjects.manualGroup()
			) );

			var result = session.createQuery(
					"SELECT g.accessGroupId, g.name FROM VercelAccessGroup g WHERE g.teamId = 'team_aaaaaa'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}
}
