/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.blazebit.query.QueryContext;
import com.blazebit.query.TypeReference;
import com.blazebit.query.impl.QueryContextBuilderImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Scaleway security group and rule queries — detecting permissive firewall configurations.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class ScalewaySecurityGroupTests {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new ScalewaySchemaProvider() );
		builder.registerSchemaObjectAlias( ScalewaySecurityGroup.class, "ScalewaySecurityGroup" );
		builder.registerSchemaObjectAlias( ScalewaySecurityGroupRule.class, "ScalewaySecurityGroupRule" );
		CONTEXT = builder.build();
	}

	private static ScalewaySecurityGroup strictGroup() {
		return new ScalewaySecurityGroup(
				"sg-001",
				"prod-firewall",
				"Production server firewall",
				"fr-par-1",
				"org-abc",
				"proj-xyz",
				"drop",
				"accept",
				true,
				true,
				false,
				true,
				"2024-01-15T10:00:00Z",
				"2024-06-01T12:00:00Z"
		);
	}

	private static ScalewaySecurityGroup permissiveGroup() {
		return new ScalewaySecurityGroup(
				"sg-002",
				"dev-open-firewall",
				"Dev server — open inbound",
				"nl-ams-1",
				"org-abc",
				"proj-dev",
				"accept",
				"accept",
				true,
				true,
				false,
				false,
				"2024-03-01T10:00:00Z",
				"2024-07-01T12:00:00Z"
		);
	}

	private static ScalewaySecurityGroup nonStatefulGroup() {
		return new ScalewaySecurityGroup(
				"sg-003",
				"legacy-stateless",
				"Legacy stateless firewall",
				"fr-par-2",
				"org-abc",
				"proj-xyz",
				"drop",
				"drop",
				false,
				false,
				false,
				false,
				"2023-01-01T10:00:00Z",
				"2024-01-01T12:00:00Z"
		);
	}

	private static ScalewaySecurityGroupRule allowAllInbound() {
		return new ScalewaySecurityGroupRule(
				"rule-001",
				"sg-002",
				"nl-ams-1",
				1,
				"ANY",
				"inbound",
				"accept",
				"0.0.0.0/0",
				null,
				null,
				true
		);
	}

	private static ScalewaySecurityGroupRule allowSshInbound() {
		return new ScalewaySecurityGroupRule(
				"rule-002",
				"sg-001",
				"fr-par-1",
				1,
				"TCP",
				"inbound",
				"accept",
				"10.0.0.0/8",
				22,
				22,
				true
		);
	}

	private static ScalewaySecurityGroupRule allowHttpsInbound() {
		return new ScalewaySecurityGroupRule(
				"rule-003",
				"sg-001",
				"fr-par-1",
				2,
				"TCP",
				"inbound",
				"accept",
				"0.0.0.0/0",
				443,
				443,
				true
		);
	}

	@Test
	void should_return_all_security_groups() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewaySecurityGroup.class, List.of( strictGroup(), permissiveGroup(), nonStatefulGroup() ) );

			var result = session.createQuery(
					"SELECT sg.id, sg.name, sg.inboundDefaultPolicy FROM ScalewaySecurityGroup sg",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}

	@Test
	void should_find_permissive_inbound_groups() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewaySecurityGroup.class, List.of( strictGroup(), permissiveGroup(), nonStatefulGroup() ) );

			var result = session.createQuery(
					"SELECT sg.id, sg.name, sg.zone FROM ScalewaySecurityGroup sg WHERE sg.inboundDefaultPolicy = 'accept'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "dev-open-firewall" );
		}
	}

	@Test
	void should_find_non_stateful_groups() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewaySecurityGroup.class, List.of( strictGroup(), permissiveGroup(), nonStatefulGroup() ) );

			var result = session.createQuery(
					"SELECT sg.id, sg.name FROM ScalewaySecurityGroup sg WHERE sg.stateful = false",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "legacy-stateless" );
		}
	}

	@Test
	void should_find_groups_with_default_security_disabled() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewaySecurityGroup.class, List.of( strictGroup(), permissiveGroup(), nonStatefulGroup() ) );

			var result = session.createQuery(
					"SELECT sg.id, sg.name FROM ScalewaySecurityGroup sg WHERE sg.enableDefaultSecurity = false",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "legacy-stateless" );
		}
	}

	@Test
	void should_find_rules_open_to_all_ips() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewaySecurityGroupRule.class, List.of( allowAllInbound(), allowSshInbound(), allowHttpsInbound() ) );

			var result = session.createQuery(
					"SELECT r.id, r.securityGroupId, r.protocol, r.direction FROM ScalewaySecurityGroupRule r WHERE r.ipRange = '0.0.0.0/0' AND r.direction = 'inbound' AND r.action = 'accept'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_allow_all_inbound_rules() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewaySecurityGroupRule.class, List.of( allowAllInbound(), allowSshInbound(), allowHttpsInbound() ) );

			var result = session.createQuery(
					"""
					SELECT r.id, r.securityGroupId
					FROM ScalewaySecurityGroupRule r
					WHERE r.direction = 'inbound'
					AND r.action = 'accept'
					AND r.ipRange = '0.0.0.0/0'
					AND r.destPortFrom IS NULL
					AND r.destPortTo IS NULL
					""",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "securityGroupId" ) ).isEqualTo( "sg-002" );
		}
	}

	@Test
	void should_find_ssh_exposed_to_internet() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewaySecurityGroupRule.class, List.of( allowAllInbound(), allowSshInbound(), allowHttpsInbound() ) );

			var result = session.createQuery(
					"""
					SELECT r.id, r.securityGroupId, r.ipRange
					FROM ScalewaySecurityGroupRule r
					WHERE r.direction = 'inbound'
					AND r.action = 'accept'
					AND r.destPortFrom <= 22 AND r.destPortTo >= 22
					AND r.ipRange = '0.0.0.0/0'
					""",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).isEmpty();
		}
	}
}
