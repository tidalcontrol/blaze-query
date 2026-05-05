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
 * configuration change monitoring against the {@code HydratedCriticalAction}
 * schema.
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
				1, "admin@example.com", "203.0.113.1", "Munich, Germany", "DE", "BY",
				"user-5", "https://app.hubspot.com/security/1" );
	}

	private static HubspotSecurityActivity mfaDisabled() {
		return new HubspotSecurityActivity( "sec-2", "MFA_DISABLED", "2024-03-01T09:00:00Z",
				1, "admin@example.com", "203.0.113.1", "Munich, Germany", "DE", "BY",
				"user-6", "https://app.hubspot.com/security/2" );
	}

	private static HubspotSecurityActivity ssoConfigured() {
		return new HubspotSecurityActivity( "sec-3", "SSO_CONFIGURED", "2024-03-01T10:00:00Z",
				1, "admin@example.com", "203.0.113.1", "Munich, Germany", "DE", "BY",
				null, "https://app.hubspot.com/security/3" );
	}

	private static HubspotSecurityActivity apiTokenCreated() {
		return new HubspotSecurityActivity( "sec-4", "API_TOKEN_CREATED", "2024-03-01T11:00:00Z",
				2, "developer@example.com", "198.51.100.5", "San Francisco, US", "US", "CA",
				null, "https://app.hubspot.com/security/4" );
	}

	private static HubspotSecurityActivity permissionChanged() {
		return new HubspotSecurityActivity( "sec-5", "PERMISSION_CHANGED", "2024-03-01T12:00:00Z",
				1, "admin@example.com", "203.0.113.1", "Munich, Germany", "DE", "BY",
				"user-7", "https://app.hubspot.com/security/5" );
	}

	// --- tests ---------------------------------------------------------------

	@Test
	void should_return_all_security_events() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotSecurityActivity.class,
					List.of( mfaEnabled(), mfaDisabled(), ssoConfigured(), apiTokenCreated(), permissionChanged() ) );

			var result = session.createQuery(
					"SELECT s.id, s.type, s.createdAt FROM HubspotSecurityActivity s",
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
					"SELECT s.id, s.type, s.objectId FROM HubspotSecurityActivity s"
							+ " WHERE s.type IN ('MFA_ENABLED', 'MFA_DISABLED')",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_mfa_disabled_events() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotSecurityActivity.class,
					List.of( mfaEnabled(), mfaDisabled(), ssoConfigured(), apiTokenCreated(), permissionChanged() ) );

			var result = session.createQuery(
					"SELECT s.id, s.actingUser, s.objectId, s.createdAt FROM HubspotSecurityActivity s"
							+ " WHERE s.type = 'MFA_DISABLED'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "actingUser" ) ).isEqualTo( "admin@example.com" );
		}
	}

	@Test
	void should_find_sso_configuration_changes() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotSecurityActivity.class,
					List.of( mfaEnabled(), mfaDisabled(), ssoConfigured(), apiTokenCreated(), permissionChanged() ) );

			var result = session.createQuery(
					"SELECT s.id, s.actingUser, s.createdAt FROM HubspotSecurityActivity s"
							+ " WHERE s.type IN ('SSO_CONFIGURED', 'SSO_CHANGED')",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
		}
	}

	@Test
	void should_find_api_token_events() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotSecurityActivity.class,
					List.of( mfaEnabled(), mfaDisabled(), ssoConfigured(), apiTokenCreated(), permissionChanged() ) );

			var result = session.createQuery(
					"SELECT s.id, s.actingUser FROM HubspotSecurityActivity s"
							+ " WHERE s.type IN ('API_TOKEN_CREATED', 'API_TOKEN_REVOKED')",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
		}
	}

	@Test
	void should_find_events_by_acting_user() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotSecurityActivity.class,
					List.of( mfaEnabled(), mfaDisabled(), ssoConfigured(), apiTokenCreated(), permissionChanged() ) );

			var result = session.createQuery(
					"SELECT s.id, s.type FROM HubspotSecurityActivity s"
							+ " WHERE s.userId = 1",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 4 );
		}
	}

	@Test
	void should_find_permission_change_events() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotSecurityActivity.class,
					List.of( mfaEnabled(), mfaDisabled(), ssoConfigured(), apiTokenCreated(), permissionChanged() ) );

			var result = session.createQuery(
					"SELECT s.id, s.actingUser, s.objectId FROM HubspotSecurityActivity s"
							+ " WHERE s.type = 'PERMISSION_CHANGED'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "objectId" ) ).isEqualTo( "user-7" );
		}
	}
}
