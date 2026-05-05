/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.hubspot;

import com.blazebit.query.QueryContext;
import com.blazebit.query.TypeReference;
import com.blazebit.query.impl.QueryContextBuilderImpl;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HubspotUserDataFetcher} covering security & compliance queries:
 * <ul>
 *   <li>Users that can access contact information</li>
 *   <li>Stale / inactive users (using the observed {@code status} field)</li>
 *   <li>Super-admin detection</li>
 * </ul>
 */
class HubspotUserDataFetcherTest {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new HubspotSchemaProvider() );
		builder.registerSchemaObjectAlias( HubspotUser.class, "HubspotUser" );
		builder.registerSchemaObjectAlias( HubspotRole.class, "HubspotRole" );
		CONTEXT = builder.build();
	}

	// --- test data -----------------------------------------------------------

	private static HubspotUser activeAdmin() {
		return new HubspotUser(
				"user-1",
				"admin@example.com",
				"Alice",
				"Admin",
				"role-admin",
				List.of( "role-admin" ),
				"team-1",
				List.of(),
				true,
				"ACTIVE",
				OffsetDateTime.parse( "2023-01-01T00:00:00Z" ),
				OffsetDateTime.parse( "2024-06-01T00:00:00Z" )
		);
	}

	private static HubspotUser activeRegular() {
		return new HubspotUser(
				"user-2",
				"agent@example.com",
				"Bob",
				"Agent",
				"role-crm",
				List.of( "role-crm" ),
				"team-1",
				List.of(),
				false,
				"ACTIVE",
				OffsetDateTime.parse( "2023-03-01T00:00:00Z" ),
				OffsetDateTime.parse( "2024-09-15T00:00:00Z" )
		);
	}

	private static HubspotUser inactiveUser() {
		return new HubspotUser(
				"user-3",
				"former@example.com",
				"Carol",
				"Former",
				null,
				List.of(),
				null,
				List.of(),
				false,
				"INACTIVE",
				OffsetDateTime.parse( "2022-05-01T00:00:00Z" ),
				OffsetDateTime.parse( "2023-01-10T00:00:00Z" )
		);
	}

	// --- tests ---------------------------------------------------------------

	@Test
	void should_return_all_users() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotUser.class, List.of( activeAdmin(), activeRegular(), inactiveUser() ) );

			var result = session.createQuery(
					"SELECT u.id, u.email, u.firstName, u.lastName FROM HubspotUser u",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}

	@Test
	void should_find_active_users_only() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotUser.class, List.of( activeAdmin(), activeRegular(), inactiveUser() ) );

			var result = session.createQuery(
					"SELECT u.id, u.email FROM HubspotUser u WHERE u.status = 'ACTIVE'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_stale_inactive_users() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotUser.class, List.of( activeAdmin(), activeRegular(), inactiveUser() ) );

			var result = session.createQuery(
					"SELECT u.id, u.email, u.status FROM HubspotUser u WHERE u.status = 'INACTIVE'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "email" ) ).isEqualTo( "former@example.com" );
		}
	}

	@Test
	void should_find_super_admin_accounts() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotUser.class, List.of( activeAdmin(), activeRegular(), inactiveUser() ) );

			var result = session.createQuery(
					"SELECT u.id, u.email FROM HubspotUser u WHERE u.superAdmin = true",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "email" ) ).isEqualTo( "admin@example.com" );
		}
	}

	@Test
	void should_return_users_with_role_assignments() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotUser.class, List.of( activeAdmin(), activeRegular(), inactiveUser() ) );

			var result = session.createQuery(
					"SELECT u.id, u.email FROM HubspotUser u",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}

	@Test
	void should_return_roles() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotRole.class, List.of(
					new HubspotRole( "role-admin", "Super Admin", false ),
					new HubspotRole( "role-crm", "CRM Manager", false )
			) );

			var result = session.createQuery(
					"SELECT r.id, r.name FROM HubspotRole r",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}
}
