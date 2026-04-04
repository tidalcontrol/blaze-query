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
 * Tests for Scaleway SSH key queries — disabled keys and access hygiene.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public class ScalewayIamSshKeyTests {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new ScalewaySchemaProvider() );
		builder.registerSchemaObjectAlias( ScalewayIamSshKey.class, "ScalewayIamSshKey" );
		CONTEXT = builder.build();
	}

	private static ScalewayIamSshKey activeKey() {
		return new ScalewayIamSshKey(
				"key-001",
				"alice-laptop",
				"ssh-rsa AAAAB3NzaC1yc2EAAAA...",
				"SHA256:abc123...",
				false,
				"org-abc",
				"proj-xyz",
				"2024-01-10T09:00:00Z",
				"2024-01-10T09:00:00Z"
		);
	}

	private static ScalewayIamSshKey disabledKey() {
		return new ScalewayIamSshKey(
				"key-002",
				"old-server-key",
				"ssh-rsa AAAAB3NzaC1yc2EBBBB...",
				"SHA256:def456...",
				true,
				"org-abc",
				"proj-xyz",
				"2022-06-01T09:00:00Z",
				"2024-12-01T09:00:00Z"
		);
	}

	private static ScalewayIamSshKey anotherActiveKey() {
		return new ScalewayIamSshKey(
				"key-003",
				"bob-workstation",
				"ssh-ed25519 AAAAC3NzaC1lZDI1NTE5...",
				"SHA256:ghi789...",
				false,
				"org-abc",
				"proj-abc",
				"2024-03-15T09:00:00Z",
				"2024-03-15T09:00:00Z"
		);
	}

	@Test
	void should_return_all_ssh_keys() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayIamSshKey.class, List.of( activeKey(), disabledKey(), anotherActiveKey() ) );

			var result = session.createQuery(
					"SELECT k.id, k.name, k.disabled FROM ScalewayIamSshKey k",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}

	@Test
	void should_find_disabled_keys() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayIamSshKey.class, List.of( activeKey(), disabledKey(), anotherActiveKey() ) );

			var result = session.createQuery(
					"SELECT k.id, k.name FROM ScalewayIamSshKey k WHERE k.disabled = true",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "old-server-key" );
		}
	}

	@Test
	void should_find_active_keys_per_project() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayIamSshKey.class, List.of( activeKey(), disabledKey(), anotherActiveKey() ) );

			var result = session.createQuery(
					"SELECT k.id, k.name, k.projectId FROM ScalewayIamSshKey k WHERE k.disabled = false AND k.projectId = 'proj-xyz'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "alice-laptop" );
		}
	}

	@Test
	void should_count_keys_per_project() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayIamSshKey.class, List.of( activeKey(), disabledKey(), anotherActiveKey() ) );

			var result = session.createQuery(
					"SELECT k.projectId, COUNT(*) AS total FROM ScalewayIamSshKey k WHERE k.disabled = false GROUP BY k.projectId",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}
}
