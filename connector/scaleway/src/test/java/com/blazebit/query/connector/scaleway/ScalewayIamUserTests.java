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
 * Tests for Scaleway IAM user queries — MFA compliance, account status, and user classification.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class ScalewayIamUserTests {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new ScalewaySchemaProvider() );
		builder.registerSchemaObjectAlias( ScalewayIamUser.class, "ScalewayIamUser" );
		CONTEXT = builder.build();
	}

	private static ScalewayIamUser activeUserWithMfa() {
		return new ScalewayIamUser(
				"usr-001",
				"alice@example.com",
				"org-abc",
				"active",
				true,
				"guest",
				"2024-01-10T09:00:00Z",
				"2024-06-01T12:00:00Z",
				"2026-03-15T08:30:00Z"
		);
	}

	private static ScalewayIamUser activeUserWithoutMfa() {
		return new ScalewayIamUser(
				"usr-002",
				"bob@example.com",
				"org-abc",
				"active",
				false,
				"guest",
				"2024-02-01T09:00:00Z",
				"2024-07-01T12:00:00Z",
				"2026-02-20T10:00:00Z"
		);
	}

	private static ScalewayIamUser inactiveUser() {
		return new ScalewayIamUser(
				"usr-003",
				"charlie@example.com",
				"org-abc",
				"inactive",
				false,
				"guest",
				"2023-05-01T09:00:00Z",
				"2024-01-01T12:00:00Z",
				null
		);
	}

	private static ScalewayIamUser ownerUser() {
		return new ScalewayIamUser(
				"usr-004",
				"owner@example.com",
				"org-abc",
				"active",
				true,
				"owner",
				"2023-01-01T09:00:00Z",
				"2024-01-01T12:00:00Z",
				"2026-04-01T07:00:00Z"
		);
	}

	@Test
	void should_return_all_users() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayIamUser.class, List.of( activeUserWithMfa(), activeUserWithoutMfa(), inactiveUser(), ownerUser() ) );

			var result = session.createQuery(
					"SELECT u.id, u.email, u.status, u.mfa FROM ScalewayIamUser u",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 4 );
		}
	}

	@Test
	void should_find_active_users_without_mfa() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayIamUser.class, List.of( activeUserWithMfa(), activeUserWithoutMfa(), inactiveUser(), ownerUser() ) );

			var result = session.createQuery(
					"SELECT u.id, u.email FROM ScalewayIamUser u WHERE u.mfa = false AND u.status = 'active'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "email" ) ).isEqualTo( "bob@example.com" );
		}
	}

	@Test
	void should_find_inactive_users() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayIamUser.class, List.of( activeUserWithMfa(), activeUserWithoutMfa(), inactiveUser(), ownerUser() ) );

			var result = session.createQuery(
					"SELECT u.id, u.email FROM ScalewayIamUser u WHERE u.status = 'inactive'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "email" ) ).isEqualTo( "charlie@example.com" );
		}
	}

	@Test
	void should_find_owner_accounts() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayIamUser.class, List.of( activeUserWithMfa(), activeUserWithoutMfa(), inactiveUser(), ownerUser() ) );

			var result = session.createQuery(
					"SELECT u.id, u.email, u.mfa FROM ScalewayIamUser u WHERE u.type = 'owner'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "email" ) ).isEqualTo( "owner@example.com" );
		}
	}

	@Test
	void should_count_users_by_mfa_status() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayIamUser.class, List.of( activeUserWithMfa(), activeUserWithoutMfa(), inactiveUser(), ownerUser() ) );

			var result = session.createQuery(
					"SELECT u.mfa, COUNT(*) AS cnt FROM ScalewayIamUser u GROUP BY u.mfa",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_active_users_in_organization() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayIamUser.class, List.of( activeUserWithMfa(), activeUserWithoutMfa(), inactiveUser(), ownerUser() ) );

			var result = session.createQuery(
					"SELECT u.id, u.email, u.status FROM ScalewayIamUser u WHERE u.organizationId = 'org-abc' AND u.status = 'active'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}
}
