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

public class TeamMemberDataFetcherTest {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new VercelSchemaProvider() );
		builder.registerSchemaObjectAlias( TeamMember.class, "VercelTeamMember" );
		CONTEXT = builder.build();
	}

	@Test
	void should_return_all_members() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( TeamMember.class, List.of(
					VercelTestObjects.ownerMember(),
					VercelTestObjects.developerMember(),
					VercelTestObjects.unconfirmedMember()
			) );

			var result = session.createQuery(
					"SELECT m.uid, m.email, m.role FROM VercelTeamMember m",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}

	@Test
	void should_find_owners() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( TeamMember.class, List.of(
					VercelTestObjects.ownerMember(),
					VercelTestObjects.developerMember(),
					VercelTestObjects.unconfirmedMember()
			) );

			var result = session.createQuery(
					"SELECT m.uid, m.email FROM VercelTeamMember m WHERE m.role = 'OWNER'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "email" ) ).isEqualTo( "alice@acme.com" );
		}
	}

	@Test
	void should_find_unconfirmed_members() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( TeamMember.class, List.of(
					VercelTestObjects.ownerMember(),
					VercelTestObjects.developerMember(),
					VercelTestObjects.unconfirmedMember()
			) );

			// Unconfirmed members have accepted an invite but not yet confirmed — potential stale invites
			var result = session.createQuery(
					"SELECT m.uid, m.email FROM VercelTeamMember m WHERE m.confirmed = false",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "email" ) ).isEqualTo( "charlie@acme.com" );
		}
	}

	@Test
	void should_find_saml_provisioned_members() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( TeamMember.class, List.of(
					VercelTestObjects.ownerMember(),
					VercelTestObjects.developerMember(),
					VercelTestObjects.unconfirmedMember()
			) );

			var result = session.createQuery(
					"SELECT m.uid, m.email, m.joinedFrom.origin FROM VercelTeamMember m WHERE m.joinedFrom.origin = 'saml'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "email" ) ).isEqualTo( "bob@acme.com" );
		}
	}

	@Test
	void should_find_members_by_team() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( TeamMember.class, List.of(
					VercelTestObjects.ownerMember(),
					VercelTestObjects.developerMember(),
					VercelTestObjects.unconfirmedMember()
			) );

			var result = session.createQuery(
					"SELECT m.uid, m.email, m.role FROM VercelTeamMember m WHERE m.teamId = 'team_aaaaaa'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}
}
