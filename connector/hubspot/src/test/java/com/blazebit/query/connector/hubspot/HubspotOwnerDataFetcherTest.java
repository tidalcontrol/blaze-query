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
 * Tests for {@link HubspotOwnerDataFetcher} covering the compliance query
 * "users that can access contact information".
 */
class HubspotOwnerDataFetcherTest {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new HubspotSchemaProvider() );
		builder.registerSchemaObjectAlias( HubspotOwner.class, "HubspotOwner" );
		builder.registerSchemaObjectAlias( HubspotUser.class, "HubspotUser" );
		CONTEXT = builder.build();
	}

	// --- test data -----------------------------------------------------------

	private static HubspotOwner activeOwner() {
		return new HubspotOwner(
				"owner-1",
				"PERSON",
				101,
				null,
				"sales@example.com",
				"Alice",
				"Smith",
				false,
				List.of( new HubspotOwner.OwnerTeam( "team-1", "West Coast Sales", true ) ),
				OffsetDateTime.parse( "2023-01-01T00:00:00Z" ),
				OffsetDateTime.parse( "2024-03-01T00:00:00Z" )
		);
	}

	private static HubspotOwner archivedOwner() {
		return new HubspotOwner(
				"owner-2",
				"PERSON",
				null,
				102,
				"former@example.com",
				"Bob",
				"Jones",
				true,
				List.of(),
				OffsetDateTime.parse( "2022-01-01T00:00:00Z" ),
				OffsetDateTime.parse( "2023-12-01T00:00:00Z" )
		);
	}

	private static HubspotOwner queueOwner() {
		return new HubspotOwner(
				"owner-3",
				"QUEUE",
				null,
				null,
				null,
				null,
				null,
				false,
				List.of(),
				OffsetDateTime.parse( "2024-01-01T00:00:00Z" ),
				OffsetDateTime.parse( "2024-01-01T00:00:00Z" )
		);
	}

	// --- tests ---------------------------------------------------------------

	@Test
	void should_return_all_owners() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotOwner.class, List.of( activeOwner(), archivedOwner(), queueOwner() ) );

			var result = session.createQuery(
					"SELECT o.id, o.email, o.firstName, o.lastName, o.type FROM HubspotOwner o",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}

	@Test
	void should_find_active_owners_with_contact_access() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotOwner.class, List.of( activeOwner(), archivedOwner(), queueOwner() ) );

			var result = session.createQuery(
					"SELECT o.id, o.email FROM HubspotOwner o"
							+ " WHERE o.archived = false AND o.type = 'PERSON'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "email" ) ).isEqualTo( "sales@example.com" );
		}
	}

	@Test
	void should_find_archived_owners() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotOwner.class, List.of( activeOwner(), archivedOwner(), queueOwner() ) );

			var result = session.createQuery(
					"SELECT o.id, o.email FROM HubspotOwner o WHERE o.archived = true",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "email" ) ).isEqualTo( "former@example.com" );
		}
	}

	@Test
	void should_distinguish_queue_owners_from_person_owners() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotOwner.class, List.of( activeOwner(), archivedOwner(), queueOwner() ) );

			var result = session.createQuery(
					"SELECT o.id, o.type FROM HubspotOwner o WHERE o.type = 'QUEUE'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
		}
	}

	@Test
	void should_cross_reference_owners_and_users() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotOwner.class, List.of( activeOwner(), archivedOwner() ) );
			session.put( HubspotUser.class, List.of(
					new HubspotUser( "user-101", "sales@example.com", "Alice", "Smith",
							"role-crm", List.of( "role-crm" ), "team-1", List.of(),
							false, "ACTIVE",
							OffsetDateTime.parse( "2023-01-01T00:00:00Z" ),
							OffsetDateTime.parse( "2024-03-01T00:00:00Z" ) )
			) );

			var owners = session.createQuery(
					"SELECT o.email FROM HubspotOwner o WHERE o.archived = false",
					new TypeReference<Map<String, Object>>() {} ).getResultList();
			var users = session.createQuery(
					"SELECT u.email, u.status FROM HubspotUser u WHERE u.status = 'ACTIVE'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( owners ).hasSize( 1 );
			assertThat( users ).hasSize( 1 );
			assertThat( owners.get( 0 ).get( "email" ) )
					.isEqualTo( users.get( 0 ).get( "email" ) );
		}
	}
}
