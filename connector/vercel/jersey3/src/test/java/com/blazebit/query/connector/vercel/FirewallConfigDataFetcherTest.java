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

public class FirewallConfigDataFetcherTest {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new VercelSchemaProvider() );
		builder.registerSchemaObjectAlias( FirewallConfig.class, "VercelFirewallConfig" );
		CONTEXT = builder.build();
	}

	@Test
	void should_return_all_firewall_configs() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( FirewallConfig.class, List.of(
					VercelTestObjects.firewallEnabled(),
					VercelTestObjects.firewallDisabled()
			) );

			var result = session.createQuery(
					"SELECT f.id, f.projectId, f.firewallEnabled FROM VercelFirewallConfig f",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_projects_with_firewall_disabled() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( FirewallConfig.class, List.of(
					VercelTestObjects.firewallEnabled(),
					VercelTestObjects.firewallDisabled()
			) );

			var result = session.createQuery(
					"SELECT f.projectId FROM VercelFirewallConfig f WHERE f.firewallEnabled = false OR f.firewallEnabled IS NULL",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "projectId" ) ).isEqualTo( "prj_no_waf" );
		}
	}

	@Test
	void should_find_projects_with_bot_protection_disabled() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( FirewallConfig.class, List.of(
					VercelTestObjects.firewallEnabled(),
					VercelTestObjects.firewallDisabled()
			) );

			var result = session.createQuery(
					"SELECT f.projectId FROM VercelFirewallConfig f WHERE f.botIdEnabled = false OR f.botIdEnabled IS NULL",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "projectId" ) ).isEqualTo( "prj_no_waf" );
		}
	}
}
