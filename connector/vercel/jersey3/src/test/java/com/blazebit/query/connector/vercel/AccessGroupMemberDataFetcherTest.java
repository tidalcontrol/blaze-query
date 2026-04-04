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

public class AccessGroupMemberDataFetcherTest {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new VercelSchemaProvider() );
		builder.registerSchemaObjectAlias( AccessGroupMember.class, "VercelAccessGroupMember" );
		CONTEXT = builder.build();
	}

	@Test
	void should_return_all_access_group_members() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( AccessGroupMember.class, List.of(
					VercelTestObjects.accessGroupAdminMember(),
					VercelTestObjects.accessGroupViewerMember()
			) );

			var result = session.createQuery(
					"SELECT m.uid, m.email, m.role, m.accessGroupId FROM VercelAccessGroupMember m",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_admin_members_in_access_groups() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( AccessGroupMember.class, List.of(
					VercelTestObjects.accessGroupAdminMember(),
					VercelTestObjects.accessGroupViewerMember()
			) );

			var result = session.createQuery(
					"SELECT m.uid, m.email, m.accessGroupId FROM VercelAccessGroupMember m WHERE m.role = 'ADMIN'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "email" ) ).isEqualTo( "eve@acme.com" );
		}
	}

	@Test
	void should_find_members_by_access_group() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( AccessGroupMember.class, List.of(
					VercelTestObjects.accessGroupAdminMember(),
					VercelTestObjects.accessGroupViewerMember()
			) );

			var result = session.createQuery(
					"SELECT m.uid, m.email FROM VercelAccessGroupMember m WHERE m.accessGroupId = 'ag_111'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}
}
