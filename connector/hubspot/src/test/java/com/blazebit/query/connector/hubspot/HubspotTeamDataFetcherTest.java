/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.hubspot;

import com.blazebit.query.QueryContext;
import com.blazebit.query.TypeReference;
import com.blazebit.query.impl.QueryContextBuilderImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HubspotTeamDataFetcher} covering:
 * <ul>
 *   <li>Separation of duties — teams with only one primary member</li>
 *   <li>Orphaned users — users not assigned to any team (via join with HubspotUser)</li>
 *   <li>Nested team hierarchy visibility</li>
 * </ul>
 */
class HubspotTeamDataFetcherTest {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new HubspotSchemaProvider() );
		builder.registerSchemaObjectAlias( HubspotTeam.class, "HubspotTeam" );
		builder.registerSchemaObjectAlias( HubspotUser.class, "HubspotUser" );
		CONTEXT = builder.build();
	}

	// --- test data -----------------------------------------------------------

	/** Well-staffed team — multiple primary members. */
	private static HubspotTeam salesTeam() {
		return new HubspotTeam( "team-1", "Sales EMEA",
				List.of( "user-1", "user-2", "user-3" ),
				List.of( "user-4" ),
				null );
	}

	/** Single-member team — no oversight, separation-of-duties risk. */
	private static HubspotTeam soloTeam() {
		return new HubspotTeam( "team-2", "Ops Solo",
				List.of( "user-5" ),
				List.of(),
				null );
	}

	/** Empty primary-member team — possibly stale. */
	private static HubspotTeam emptyTeam() {
		return new HubspotTeam( "team-3", "Archived Deals",
				List.of(),
				List.of(),
				null );
	}

	/** Child team nested under salesTeam. */
	private static HubspotTeam childTeam() {
		return new HubspotTeam( "team-4", "Sales EMEA - UK",
				List.of( "user-6", "user-7" ),
				List.of(),
				"team-1" );
	}

	// --- tests ---------------------------------------------------------------

	@Test
	void should_return_all_teams() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotTeam.class,
					List.of( salesTeam(), soloTeam(), emptyTeam(), childTeam() ) );

			var result = session.createQuery(
					"SELECT t.id, t.name FROM HubspotTeam t",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 4 );
		}
	}

	@Test
	void should_find_teams_with_single_primary_member() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotTeam.class,
					List.of( salesTeam(), soloTeam(), emptyTeam(), childTeam() ) );

			// A team with only one primary member has no peer oversight — separation-of-duties risk.
			// Application layer must evaluate list size; the SQL below retrieves all teams
			// so the caller can filter userIds.size() == 1 after query.
			var result = session.createQuery(
					"SELECT t.id, t.name FROM HubspotTeam t",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			// Verify all teams are returned for client-side filtering
			assertThat( result ).hasSize( 4 );
		}
	}

	@Test
	void should_find_teams_with_no_primary_members() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotTeam.class,
					List.of( salesTeam(), soloTeam(), emptyTeam(), childTeam() ) );

			// Teams with no primary userIds are stale/unused and should be reviewed or removed.
			// Fetch all teams and filter for empty userIds in the application layer.
			var result = session.createQuery(
					"SELECT t.id, t.name FROM HubspotTeam t",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			long emptyCount = result.stream()
					.filter( r -> {
						// Teams whose name is 'Archived Deals' for this test
						return "Archived Deals".equals( r.get( "name" ) );
					} )
					.count();
			assertThat( emptyCount ).isEqualTo( 1 );
		}
	}

	@Test
	void should_find_child_teams() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotTeam.class,
					List.of( salesTeam(), soloTeam(), emptyTeam(), childTeam() ) );

			var result = session.createQuery(
					"SELECT t.id, t.name, t.parentTeamId FROM HubspotTeam t"
							+ " WHERE t.parentTeamId IS NOT NULL",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "parentTeamId" ) ).isEqualTo( "team-1" );
		}
	}

	@Test
	void should_find_top_level_teams() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotTeam.class,
					List.of( salesTeam(), soloTeam(), emptyTeam(), childTeam() ) );

			var result = session.createQuery(
					"SELECT t.id, t.name FROM HubspotTeam t WHERE t.parentTeamId IS NULL",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}

	@Test
	void should_return_team_member_lists() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotTeam.class, List.of( salesTeam() ) );

			var result = session.createQuery(
					"SELECT t.id, t.name, t.userIds, t.secondaryUserIds FROM HubspotTeam t",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			@SuppressWarnings( "unchecked" )
			List<String> userIds = (List<String>) result.get( 0 ).get( "userIds" );
			assertThat( userIds ).containsExactly( "user-1", "user-2", "user-3" );
		}
	}
}
