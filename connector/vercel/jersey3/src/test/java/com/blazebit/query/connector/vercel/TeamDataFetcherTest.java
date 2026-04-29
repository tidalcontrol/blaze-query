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

public class TeamDataFetcherTest {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new VercelSchemaProvider() );
		builder.registerSchemaObjectAlias( Team.class, "VercelTeam" );
		CONTEXT = builder.build();
	}

	@Test
	void should_return_all_teams() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( Team.class, List.of(
					VercelTestObjects.teamWithSamlEnforced(),
					VercelTestObjects.teamWithSamlNotEnforced()
			) );

			var result = session.createQuery(
					"SELECT t.id, t.slug, t.name FROM VercelTeam t",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_teams_without_saml_enforced() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( Team.class, List.of(
					VercelTestObjects.teamWithSamlEnforced(),
					VercelTestObjects.teamWithSamlNotEnforced()
			) );

			// Teams where SAML is not enforced — a compliance risk
			var result = session.createQuery(
					"SELECT t.id, t.slug FROM VercelTeam t WHERE t.saml.enforced = false OR t.saml IS NULL",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "slug" ) ).isEqualTo( "acme-eng" );
		}
	}

	@Test
	void should_find_teams_with_ip_addresses_exposed() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( Team.class, List.of(
					VercelTestObjects.teamWithSamlEnforced(),
					VercelTestObjects.teamWithSamlNotEnforced()
			) );

			var result = session.createQuery(
					"SELECT t.id, t.slug FROM VercelTeam t WHERE t.hideIpAddresses = false OR t.hideIpAddresses IS NULL",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "slug" ) ).isEqualTo( "acme-eng" );
		}
	}

	@Test
	void should_find_teams_with_sensitive_env_policy_off() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( Team.class, List.of(
					VercelTestObjects.teamWithSamlEnforced(),
					VercelTestObjects.teamWithSamlNotEnforced()
			) );

			var result = session.createQuery(
					"SELECT t.id, t.slug FROM VercelTeam t WHERE t.sensitiveEnvironmentVariablePolicy = 'off'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "slug" ) ).isEqualTo( "acme-eng" );
		}
	}
}
