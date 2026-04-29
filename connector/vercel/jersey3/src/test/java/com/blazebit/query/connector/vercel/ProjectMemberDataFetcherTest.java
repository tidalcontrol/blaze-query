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

public class ProjectMemberDataFetcherTest {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new VercelSchemaProvider() );
		builder.registerSchemaObjectAlias( ProjectMember.class, "VercelProjectMember" );
		CONTEXT = builder.build();
	}

	@Test
	void should_return_all_project_members() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( ProjectMember.class, List.of(
					VercelTestObjects.projectAdmin(),
					VercelTestObjects.projectViewer()
			) );

			var result = session.createQuery(
					"SELECT m.uid, m.email, m.role, m.projectId FROM VercelProjectMember m",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_project_admins() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( ProjectMember.class, List.of(
					VercelTestObjects.projectAdmin(),
					VercelTestObjects.projectViewer()
			) );

			var result = session.createQuery(
					"SELECT m.uid, m.email, m.projectId FROM VercelProjectMember m WHERE m.role = 'ADMIN'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "email" ) ).isEqualTo( "dave@acme.com" );
		}
	}

	@Test
	void should_find_members_with_elevated_project_role() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( ProjectMember.class, List.of(
					VercelTestObjects.projectAdmin(),
					VercelTestObjects.projectViewer()
			) );

			// Members whose project role is ADMIN but team role is not OWNER — privilege escalation
			var result = session.createQuery(
					"SELECT m.uid, m.email, m.role, m.teamRole FROM VercelProjectMember m WHERE m.role = 'ADMIN' AND m.teamRole <> 'OWNER'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "email" ) ).isEqualTo( "dave@acme.com" );
		}
	}
}
