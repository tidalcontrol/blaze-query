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
 * Tests for {@link HubspotAuditLogDataFetcher} covering the compliance query
 * "logging and monitoring".
 */
class HubspotAuditLogDataFetcherTest {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new HubspotSchemaProvider() );
		builder.registerSchemaObjectAlias( HubspotAuditLog.class, "HubspotAuditLog" );
		CONTEXT = builder.build();
	}

	// --- test data -----------------------------------------------------------

	private static HubspotAuditLog contactUpdated() {
		return new HubspotAuditLog( "evt-1", "CONTACT", "UPDATED", "Contact updated",
				"contact-100", "CONTACT", "2024-03-01T10:00:00Z",
				new HubspotAuditLog.ActingUser( "user-1", "alice@example.com" ) );
	}

	private static HubspotAuditLog contactDeleted() {
		return new HubspotAuditLog( "evt-2", "CONTACT", "DELETED", "Contact deleted",
				"contact-200", "CONTACT", "2024-03-01T11:00:00Z",
				new HubspotAuditLog.ActingUser( "user-2", "bob@example.com" ) );
	}

	private static HubspotAuditLog userCreated() {
		return new HubspotAuditLog( "evt-3", "USER", "CREATED", "User added",
				"user-300", "USER", "2024-03-01T09:00:00Z",
				new HubspotAuditLog.ActingUser( "user-1", "alice@example.com" ) );
	}

	private static HubspotAuditLog integrationCreated() {
		return new HubspotAuditLog( "evt-4", "INTEGRATION", "CREATED", "Integration added",
				"integration-400", "INTEGRATION", "2024-03-01T08:00:00Z",
				new HubspotAuditLog.ActingUser( "user-1", "alice@example.com" ) );
	}

	// --- tests ---------------------------------------------------------------

	@Test
	void should_return_all_audit_logs() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotAuditLog.class,
					List.of( contactUpdated(), contactDeleted(), userCreated(), integrationCreated() ) );

			var result = session.createQuery(
					"SELECT l.id, l.category, l.subCategory, l.occurredAt FROM HubspotAuditLog l",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 4 );
		}
	}

	@Test
	void should_find_contact_data_access_events() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotAuditLog.class,
					List.of( contactUpdated(), contactDeleted(), userCreated(), integrationCreated() ) );

			var result = session.createQuery(
					"SELECT l.id, l.targetObjectId, l.subCategory FROM HubspotAuditLog l"
							+ " WHERE l.targetObjectType = 'CONTACT'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_deletion_events() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotAuditLog.class,
					List.of( contactUpdated(), contactDeleted(), userCreated(), integrationCreated() ) );

			var result = session.createQuery(
					"SELECT l.id, l.targetObjectType, l.targetObjectId FROM HubspotAuditLog l"
							+ " WHERE l.subCategory = 'DELETED'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "targetObjectType" ) ).isEqualTo( "CONTACT" );
		}
	}

	@Test
	void should_find_user_lifecycle_events() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotAuditLog.class,
					List.of( contactUpdated(), contactDeleted(), userCreated(), integrationCreated() ) );

			var result = session.createQuery(
					"SELECT l.id, l.targetObjectId, l.subCategory FROM HubspotAuditLog l"
							+ " WHERE l.category = 'USER'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "subCategory" ) ).isEqualTo( "CREATED" );
		}
	}

	@Test
	void should_find_integration_activity() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotAuditLog.class,
					List.of( contactUpdated(), contactDeleted(), userCreated(), integrationCreated() ) );

			var result = session.createQuery(
					"SELECT l.id, l.targetObjectId FROM HubspotAuditLog l"
							+ " WHERE l.category = 'INTEGRATION'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
		}
	}

	@Test
	void should_find_events_by_acting_user_email() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotAuditLog.class,
					List.of( contactUpdated(), contactDeleted(), userCreated(), integrationCreated() ) );

			var result = session.createQuery(
					"SELECT l.category, l.subCategory FROM HubspotAuditLog l"
							+ " WHERE l.actingUser.userEmail = 'alice@example.com'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}
}
