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
 * Tests for {@link HubspotSecurityActivityDataFetcher} covering security
 * configuration change monitoring.
 */
class HubspotSecurityActivityDataFetcherTest {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new HubspotSchemaProvider() );
		builder.registerSchemaObjectAlias( HubspotSecurityActivity.class, "HubspotSecurityActivity" );
		CONTEXT = builder.build();
	}

	// --- test data -----------------------------------------------------------

	private static HubspotSecurityActivity mfaEnabled() {
		return new HubspotSecurityActivity( "sec-1", "MFA_ENABLED", "2024-03-01T08:00:00Z",
				"user-1", "admin@example.com", "user-5", "LOW", "MFA enabled for user" );
	}

	private static HubspotSecurityActivity mfaDisabled() {
		return new HubspotSecurityActivity( "sec-2", "MFA_DISABLED", "2024-03-01T09:00:00Z",
				"user-1", "admin@example.com", "user-6", "HIGH", "MFA disabled for user" );
	}

	private static HubspotSecurityActivity ssoConfigured() {
		return new HubspotSecurityActivity( "sec-3", "SSO_CONFIGURED", "2024-03-01T10:00:00Z",
				"user-1", "admin@example.com", null, "HIGH", "SSO configured" );
	}

	private static HubspotSecurityActivity apiTokenCreated() {
		return new HubspotSecurityActivity( "sec-4", "API_TOKEN_CREATED", "2024-03-01T11:00:00Z",
				"user-2", "developer@example.com", null, "MEDIUM", "New API token created" );
	}

	private static HubspotSecurityActivity permissionChanged() {
		return new HubspotSecurityActivity( "sec-5", "PERMISSION_CHANGED", "2024-03-01T12:00:00Z",
				"user-1", "admin@example.com", "user-7", "MEDIUM", "User permissions updated" );
	}

	// --- tests ---------------------------------------------------------------

	@Test
	void should_return_all_security_events() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotSecurityActivity.class,
					List.of( mfaEnabled(), mfaDisabled(), ssoConfigured(), apiTokenCreated(), permissionChanged() ) );

			var result = session.createQuery(
					"SELECT s.id, s.eventType, s.severity FROM HubspotSecurityActivity s",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 5 );
		}
	}

	@Test
	void should_find_mfa_toggle_events() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotSecurityActivity.class,
					List.of( mfaEnabled(), mfaDisabled(), ssoConfigured(), apiTokenCreated(), permissionChanged() ) );

			var result = session.createQuery(
					"SELECT s.id, s.eventType, s.affectedUserId FROM HubspotSecurityActivity s"
							+ " WHERE s.eventType IN ('MFA_ENABLED', 'MFA_DISABLED')",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_mfa_disabled_events() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotSecurityActivity.class,
					List.of( mfaEnabled(), mfaDisabled(), ssoConfigured(), apiTokenCreated(), permissionChanged() ) );

			// MFA_DISABLED is a high-risk event — someone turned off 2FA for a user
			var result = session.createQuery(
					"SELECT s.id, s.actingUserEmail, s.affectedUserId, s.occurredAt FROM HubspotSecurityActivity s"
							+ " WHERE s.eventType = 'MFA_DISABLED'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "actingUserEmail" ) ).isEqualTo( "admin@example.com" );
		}
	}

	@Test
	void should_find_sso_configuration_changes() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotSecurityActivity.class,
					List.of( mfaEnabled(), mfaDisabled(), ssoConfigured(), apiTokenCreated(), permissionChanged() ) );

			var result = session.createQuery(
					"SELECT s.id, s.actingUserEmail, s.occurredAt FROM HubspotSecurityActivity s"
							+ " WHERE s.eventType IN ('SSO_CONFIGURED', 'SSO_CHANGED')",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
		}
	}

	@Test
	void should_find_high_severity_events() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotSecurityActivity.class,
					List.of( mfaEnabled(), mfaDisabled(), ssoConfigured(), apiTokenCreated(), permissionChanged() ) );

			var result = session.createQuery(
					"SELECT s.id, s.eventType, s.actingUserEmail FROM HubspotSecurityActivity s"
							+ " WHERE s.severity = 'HIGH' OR s.severity = 'CRITICAL'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			// MFA_DISABLED and SSO_CONFIGURED are both HIGH
			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_api_token_events() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotSecurityActivity.class,
					List.of( mfaEnabled(), mfaDisabled(), ssoConfigured(), apiTokenCreated(), permissionChanged() ) );

			var result = session.createQuery(
					"SELECT s.id, s.actingUserEmail FROM HubspotSecurityActivity s"
							+ " WHERE s.eventType IN ('API_TOKEN_CREATED', 'API_TOKEN_REVOKED')",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
		}
	}

	@Test
	void should_find_permission_change_events() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotSecurityActivity.class,
					List.of( mfaEnabled(), mfaDisabled(), ssoConfigured(), apiTokenCreated(), permissionChanged() ) );

			var result = session.createQuery(
					"SELECT s.id, s.actingUserEmail, s.affectedUserId FROM HubspotSecurityActivity s"
							+ " WHERE s.eventType = 'PERMISSION_CHANGED'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "affectedUserId" ) ).isEqualTo( "user-7" );
		}
	}
}
