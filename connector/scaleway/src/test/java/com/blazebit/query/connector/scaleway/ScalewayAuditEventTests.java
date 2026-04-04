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
 * Tests for Scaleway Audit Trail queries — forbidden access and suspicious activity detection.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public class ScalewayAuditEventTests {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new ScalewaySchemaProvider() );
		builder.registerSchemaObjectAlias( ScalewayAuditEvent.class, "ScalewayAuditEvent" );
		CONTEXT = builder.build();
	}

	private static ScalewayAuditEvent successfulDeleteEvent() {
		return new ScalewayAuditEvent(
				"evt-001",
				"2026-04-01T14:00:00Z",
				"usr-001",
				"user",
				"185.42.10.5",
				"scaleway-cli/2.20.0",
				"instance.DeleteServer",
				"success",
				"instance.Server",
				"srv-001",
				"proj-xyz",
				"org-abc",
				"fr-par",
				"fr-par-1"
		);
	}

	private static ScalewayAuditEvent forbiddenIamEvent() {
		return new ScalewayAuditEvent(
				"evt-002",
				"2026-04-01T15:30:00Z",
				"app-001",
				"application",
				"10.0.0.50",
				"terraform/1.7.0",
				"iam.CreatePolicy",
				"forbidden",
				"iam.Policy",
				null,
				"proj-xyz",
				"org-abc",
				"fr-par",
				null
		);
	}

	private static ScalewayAuditEvent successfulKeyDeleteEvent() {
		return new ScalewayAuditEvent(
				"evt-003",
				"2026-04-02T08:00:00Z",
				"usr-002",
				"user",
				"91.100.50.20",
				"Mozilla/5.0",
				"keymanager.DeleteKey",
				"success",
				"keymanager.Key",
				"key-002",
				"proj-xyz",
				"org-abc",
				"nl-ams",
				null
		);
	}

	@Test
	void should_return_all_events() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayAuditEvent.class, List.of( successfulDeleteEvent(), forbiddenIamEvent(), successfulKeyDeleteEvent() ) );

			var result = session.createQuery(
					"SELECT e.id, e.apiMethod, e.status, e.principalId FROM ScalewayAuditEvent e",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}

	@Test
	void should_find_forbidden_events() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayAuditEvent.class, List.of( successfulDeleteEvent(), forbiddenIamEvent(), successfulKeyDeleteEvent() ) );

			var result = session.createQuery(
					"SELECT e.id, e.apiMethod, e.principalId, e.principalType FROM ScalewayAuditEvent e WHERE e.status = 'forbidden'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "apiMethod" ) ).isEqualTo( "iam.CreatePolicy" );
		}
	}

	@Test
	void should_find_destructive_events() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayAuditEvent.class, List.of( successfulDeleteEvent(), forbiddenIamEvent(), successfulKeyDeleteEvent() ) );

			var result = session.createQuery(
					"SELECT e.id, e.apiMethod, e.principalId, e.resourceId FROM ScalewayAuditEvent e WHERE e.apiMethod LIKE '%Delete%' AND e.status = 'success'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_iam_related_events() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayAuditEvent.class, List.of( successfulDeleteEvent(), forbiddenIamEvent(), successfulKeyDeleteEvent() ) );

			var result = session.createQuery(
					"SELECT e.id, e.apiMethod, e.status, e.sourceIp FROM ScalewayAuditEvent e WHERE e.apiMethod LIKE 'iam.%'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "status" ) ).isEqualTo( "forbidden" );
		}
	}

	@Test
	void should_count_events_by_principal() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayAuditEvent.class, List.of( successfulDeleteEvent(), forbiddenIamEvent(), successfulKeyDeleteEvent() ) );

			var result = session.createQuery(
					"SELECT e.principalId, COUNT(*) AS cnt FROM ScalewayAuditEvent e GROUP BY e.principalId",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}

	@Test
	void should_find_key_deletion_events() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayAuditEvent.class, List.of( successfulDeleteEvent(), forbiddenIamEvent(), successfulKeyDeleteEvent() ) );

			var result = session.createQuery(
					"SELECT e.id, e.principalId, e.resourceId, e.sourceIp FROM ScalewayAuditEvent e WHERE e.apiMethod LIKE 'keymanager.%'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "resourceId" ) ).isEqualTo( "key-002" );
		}
	}
}
