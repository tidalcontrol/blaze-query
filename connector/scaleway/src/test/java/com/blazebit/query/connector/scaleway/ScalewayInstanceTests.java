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
 * Tests for Scaleway instance queries — running state, public exposure, and deletion protection.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public class ScalewayInstanceTests {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new ScalewaySchemaProvider() );
		builder.registerSchemaObjectAlias( ScalewayInstance.class, "ScalewayInstance" );
		CONTEXT = builder.build();
	}

	private static ScalewayInstance runningProtectedInstance() {
		return new ScalewayInstance(
				"srv-001",
				"web-prod-1",
				"running",
				"GP1-M",
				"x86_64",
				"web-prod-1",
				"10.0.0.10",
				"51.158.10.1",
				false,
				false,
				true,
				List.of( "env:prod", "team:web" ),
				"fr-par-1",
				"org-abc",
				"proj-xyz",
				"img-001",
				"Ubuntu 22.04",
				"sg-001",
				"default-prod",
				"2024-01-15T10:00:00Z",
				"2024-06-01T12:00:00Z"
		);
	}

	private static ScalewayInstance stoppedUnprotectedInstance() {
		return new ScalewayInstance(
				"srv-002",
				"dev-server-1",
				"stopped",
				"DEV1-S",
				"x86_64",
				"dev-server-1",
				null,
				null,
				false,
				false,
				false,
				List.of( "env:dev" ),
				"nl-ams-1",
				"org-abc",
				"proj-dev",
				"img-002",
				"Debian 12",
				"sg-002",
				"default-dev",
				"2024-03-01T10:00:00Z",
				"2024-07-01T12:00:00Z"
		);
	}

	private static ScalewayInstance runningPublicIpv6Instance() {
		return new ScalewayInstance(
				"srv-003",
				"api-server-1",
				"running",
				"GP1-S",
				"x86_64",
				"api-server-1",
				"10.0.0.20",
				"163.172.50.5",
				true,
				false,
				false,
				List.of( "env:prod", "team:api" ),
				"fr-par-2",
				"org-abc",
				"proj-xyz",
				"img-001",
				"Ubuntu 22.04",
				"sg-001",
				"default-prod",
				"2024-02-01T10:00:00Z",
				"2024-08-01T12:00:00Z"
		);
	}

	@Test
	void should_return_all_instances() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayInstance.class, List.of( runningProtectedInstance(), stoppedUnprotectedInstance(), runningPublicIpv6Instance() ) );

			var result = session.createQuery(
					"SELECT i.id, i.name, i.state, i.zone FROM ScalewayInstance i",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}

	@Test
	void should_find_running_instances() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayInstance.class, List.of( runningProtectedInstance(), stoppedUnprotectedInstance(), runningPublicIpv6Instance() ) );

			var result = session.createQuery(
					"SELECT i.id, i.name FROM ScalewayInstance i WHERE i.state = 'running'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_instances_with_public_ip() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayInstance.class, List.of( runningProtectedInstance(), stoppedUnprotectedInstance(), runningPublicIpv6Instance() ) );

			var result = session.createQuery(
					"SELECT i.id, i.name, i.publicIp FROM ScalewayInstance i WHERE i.publicIp IS NOT NULL",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_unprotected_running_instances() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayInstance.class, List.of( runningProtectedInstance(), stoppedUnprotectedInstance(), runningPublicIpv6Instance() ) );

			var result = session.createQuery(
					"SELECT i.id, i.name, i.zone FROM ScalewayInstance i WHERE i.instanceProtected = false AND i.state = 'running'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "api-server-1" );
		}
	}

	@Test
	void should_find_instances_with_ipv6_enabled() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayInstance.class, List.of( runningProtectedInstance(), stoppedUnprotectedInstance(), runningPublicIpv6Instance() ) );

			var result = session.createQuery(
					"SELECT i.id, i.name FROM ScalewayInstance i WHERE i.enableIpv6 = true",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "api-server-1" );
		}
	}

	@Test
	void should_count_instances_by_zone() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayInstance.class, List.of( runningProtectedInstance(), stoppedUnprotectedInstance(), runningPublicIpv6Instance() ) );

			var result = session.createQuery(
					"SELECT i.zone, COUNT(*) AS cnt FROM ScalewayInstance i GROUP BY i.zone",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}

	@Test
	void should_find_instances_by_commercial_type() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayInstance.class, List.of( runningProtectedInstance(), stoppedUnprotectedInstance(), runningPublicIpv6Instance() ) );

			var result = session.createQuery(
					"SELECT i.id, i.name, i.commercialType FROM ScalewayInstance i WHERE i.commercialType LIKE 'GP1%'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}
}
