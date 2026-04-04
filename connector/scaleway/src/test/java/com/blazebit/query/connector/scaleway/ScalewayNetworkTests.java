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
 * Tests for Scaleway network resource queries — Load Balancers, LB Frontends, and Flexible IPs.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public class ScalewayNetworkTests {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new ScalewaySchemaProvider() );
		builder.registerSchemaObjectAlias( ScalewayLoadBalancer.class, "ScalewayLoadBalancer" );
		builder.registerSchemaObjectAlias( ScalewayLoadBalancerFrontend.class, "ScalewayLoadBalancerFrontend" );
		builder.registerSchemaObjectAlias( ScalewayFlexibleIp.class, "ScalewayFlexibleIp" );
		CONTEXT = builder.build();
	}

	// -------------------------------------------------------------------------
	// Load Balancer test objects
	// -------------------------------------------------------------------------

	private static ScalewayLoadBalancer productionLb() {
		return new ScalewayLoadBalancer(
				"lb-001",
				"prod-lb",
				"ready",
				"fr-par-1",
				"proj-xyz",
				"org-abc",
				List.of( "env:prod" ),
				"2024-01-01T00:00:00Z",
				"2026-01-01T00:00:00Z"
		);
	}

	private static ScalewayLoadBalancer stagingLb() {
		return new ScalewayLoadBalancer(
				"lb-002",
				"staging-lb",
				"ready",
				"nl-ams-1",
				"proj-xyz",
				"org-abc",
				List.of( "env:staging" ),
				"2024-06-01T00:00:00Z",
				"2026-02-01T00:00:00Z"
		);
	}

	@Test
	void should_return_all_load_balancers() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayLoadBalancer.class, List.of( productionLb(), stagingLb() ) );

			var result = session.createQuery(
					"SELECT l.id, l.name, l.status, l.zone FROM ScalewayLoadBalancer l",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_ready_load_balancers_by_zone() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayLoadBalancer.class, List.of( productionLb(), stagingLb() ) );

			var result = session.createQuery(
					"SELECT l.id, l.name FROM ScalewayLoadBalancer l WHERE l.status = 'ready' AND l.zone = 'fr-par-1'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
		}
	}

	// -------------------------------------------------------------------------
	// Load Balancer Frontend test objects
	// -------------------------------------------------------------------------

	private static ScalewayLoadBalancerFrontend httpsFrontend() {
		return new ScalewayLoadBalancerFrontend(
				"fe-001",
				"https-443",
				"lb-001",
				"fr-par-1",
				"https",
				443,
				true,
				"2024-01-01T00:00:00Z",
				"2026-01-01T00:00:00Z"
		);
	}

	private static ScalewayLoadBalancerFrontend httpFrontend() {
		return new ScalewayLoadBalancerFrontend(
				"fe-002",
				"http-80",
				"lb-001",
				"fr-par-1",
				"http",
				80,
				false,
				"2024-01-01T00:00:00Z",
				"2026-01-01T00:00:00Z"
		);
	}

	private static ScalewayLoadBalancerFrontend tcpFrontend() {
		return new ScalewayLoadBalancerFrontend(
				"fe-003",
				"tcp-8080",
				"lb-002",
				"nl-ams-1",
				"tcp",
				8080,
				false,
				"2024-06-01T00:00:00Z",
				"2026-02-01T00:00:00Z"
		);
	}

	@Test
	void should_return_all_frontends() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayLoadBalancerFrontend.class, List.of( httpsFrontend(), httpFrontend(), tcpFrontend() ) );

			var result = session.createQuery(
					"SELECT f.id, f.name, f.protocol, f.inboundPort, f.tlsEnabled FROM ScalewayLoadBalancerFrontend f",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}

	@Test
	void should_find_non_tls_frontends() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayLoadBalancerFrontend.class, List.of( httpsFrontend(), httpFrontend(), tcpFrontend() ) );

			var result = session.createQuery(
					"SELECT f.id, f.name FROM ScalewayLoadBalancerFrontend f WHERE f.tlsEnabled = false AND f.protocol = 'http'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "http-80" );
		}
	}

	@Test
	void should_find_http_frontends_on_port_80() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayLoadBalancerFrontend.class, List.of( httpsFrontend(), httpFrontend(), tcpFrontend() ) );

			var result = session.createQuery(
					"SELECT f.id, f.name FROM ScalewayLoadBalancerFrontend f WHERE f.inboundPort = 80",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
		}
	}

	// -------------------------------------------------------------------------
	// Flexible IP test objects
	// -------------------------------------------------------------------------

	private static ScalewayFlexibleIp attachedIp() {
		return new ScalewayFlexibleIp(
				"fip-001",
				"51.158.10.5",
				"attached",
				"fr-par-1",
				"proj-xyz",
				"org-abc",
				"srv-001",
				List.of( "env:prod" ),
				"2024-01-01T00:00:00Z",
				"2026-01-01T00:00:00Z"
		);
	}

	private static ScalewayFlexibleIp detachedIp() {
		return new ScalewayFlexibleIp(
				"fip-002",
				"51.158.10.6",
				"detached",
				"fr-par-1",
				"proj-xyz",
				"org-abc",
				null,
				List.of(),
				"2024-03-01T00:00:00Z",
				"2024-03-01T00:00:00Z"
		);
	}

	private static ScalewayFlexibleIp updatingIp() {
		return new ScalewayFlexibleIp(
				"fip-003",
				"51.158.20.1",
				"updating",
				"nl-ams-1",
				"proj-xyz",
				"org-abc",
				"srv-002",
				List.of(),
				"2024-06-01T00:00:00Z",
				"2026-02-01T00:00:00Z"
		);
	}

	@Test
	void should_return_all_flexible_ips() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayFlexibleIp.class, List.of( attachedIp(), detachedIp(), updatingIp() ) );

			var result = session.createQuery(
					"SELECT f.id, f.ipAddress, f.status, f.zone FROM ScalewayFlexibleIp f",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}

	@Test
	void should_find_detached_flexible_ips() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayFlexibleIp.class, List.of( attachedIp(), detachedIp(), updatingIp() ) );

			var result = session.createQuery(
					"SELECT f.id, f.ipAddress FROM ScalewayFlexibleIp f WHERE f.serverId IS NULL",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "ipAddress" ) ).isEqualTo( "51.158.10.6" );
		}
	}

	@Test
	void should_find_flexible_ips_by_zone() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayFlexibleIp.class, List.of( attachedIp(), detachedIp(), updatingIp() ) );

			var result = session.createQuery(
					"SELECT f.id, f.ipAddress FROM ScalewayFlexibleIp f WHERE f.zone = 'fr-par-1'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}
}
