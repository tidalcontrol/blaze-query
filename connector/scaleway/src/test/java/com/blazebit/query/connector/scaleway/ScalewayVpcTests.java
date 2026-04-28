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
 * Tests for Scaleway VPC and Private Network queries — network segmentation and isolation.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class ScalewayVpcTests {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new ScalewaySchemaProvider() );
		builder.registerSchemaObjectAlias( ScalewayVpc.class, "ScalewayVpc" );
		builder.registerSchemaObjectAlias( ScalewayPrivateNetwork.class, "ScalewayPrivateNetwork" );
		CONTEXT = builder.build();
	}

	private static ScalewayVpc defaultVpcNoRouting() {
		return new ScalewayVpc(
				"vpc-001",
				"default",
				"fr-par",
				"proj-xyz",
				"org-abc",
				true,   // defaultVpc
				false,
				List.of(),
				"2023-01-01T10:00:00Z",
				"2024-01-01T10:00:00Z"
		);
	}

	private static ScalewayVpc customVpcWithRouting() {
		return new ScalewayVpc(
				"vpc-002",
				"prod-network",
				"fr-par",
				"proj-xyz",
				"org-abc",
				false,  // defaultVpc
				true,
				List.of( "env:prod" ),
				"2024-01-15T10:00:00Z",
				"2026-01-01T10:00:00Z"
		);
	}

	private static ScalewayVpc defaultVpcWithRouting() {
		return new ScalewayVpc(
				"vpc-003",
				"default",
				"nl-ams",
				"proj-dev",
				"org-abc",
				true,   // defaultVpc
				true,
				List.of(),
				"2023-03-01T10:00:00Z",
				"2025-01-01T10:00:00Z"
		);
	}

	private static ScalewayPrivateNetwork prodNetwork() {
		return new ScalewayPrivateNetwork(
				"pn-001",
				"prod-private",
				"vpc-002",
				"fr-par",
				"proj-xyz",
				"org-abc",
				List.of( "10.0.0.0/20" ),
				List.of( "env:prod" ),
				"2024-01-15T10:00:00Z",
				"2026-01-01T10:00:00Z"
		);
	}

	private static ScalewayPrivateNetwork devNetwork() {
		return new ScalewayPrivateNetwork(
				"pn-002",
				"dev-private",
				"vpc-003",
				"nl-ams",
				"proj-dev",
				"org-abc",
				List.of( "192.168.0.0/24" ),
				List.of( "env:dev" ),
				"2023-06-01T10:00:00Z",
				"2024-01-01T10:00:00Z"
		);
	}

	@Test
	void should_return_all_vpcs() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayVpc.class, List.of( defaultVpcNoRouting(), customVpcWithRouting(), defaultVpcWithRouting() ) );

			var result = session.createQuery(
					"SELECT v.id, v.name, v.region, v.defaultVpc, v.routingEnabled FROM ScalewayVpc v",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}

	@Test
	void should_find_default_vpcs_with_routing_enabled() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayVpc.class, List.of( defaultVpcNoRouting(), customVpcWithRouting(), defaultVpcWithRouting() ) );

			// Default VPC with routing enabled allows all private networks to talk to each other — lateral movement risk
			var result = session.createQuery(
					"SELECT v.id, v.name, v.region FROM ScalewayVpc v WHERE v.defaultVpc = true AND v.routingEnabled = true",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "region" ) ).isEqualTo( "nl-ams" );
		}
	}

	@Test
	void should_find_vpcs_with_routing_enabled() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayVpc.class, List.of( defaultVpcNoRouting(), customVpcWithRouting(), defaultVpcWithRouting() ) );

			var result = session.createQuery(
					"SELECT v.id, v.name FROM ScalewayVpc v WHERE v.routingEnabled = true",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_return_all_private_networks() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayPrivateNetwork.class, List.of( prodNetwork(), devNetwork() ) );

			var result = session.createQuery(
					"SELECT pn.id, pn.name, pn.vpcId, pn.region FROM ScalewayPrivateNetwork pn",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_private_networks_per_vpc() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayPrivateNetwork.class, List.of( prodNetwork(), devNetwork() ) );

			var result = session.createQuery(
					"SELECT pn.id, pn.name FROM ScalewayPrivateNetwork pn WHERE pn.vpcId = 'vpc-002'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "prod-private" );
		}
	}
}
