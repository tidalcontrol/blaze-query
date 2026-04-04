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
 * Tests for {@link HubspotLoginActivityDataFetcher} covering the "2FA / MFA"
 * compliance query using the {@code mfaUsed} and {@code ssoUsed} fields.
 */
class HubspotLoginActivityDataFetcherTest {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new HubspotSchemaProvider() );
		builder.registerSchemaObjectAlias( HubspotLoginActivity.class, "HubspotLoginActivity" );
		CONTEXT = builder.build();
	}

	// --- test data -----------------------------------------------------------

	/** A successful login with both MFA and SSO used. */
	private static HubspotLoginActivity mfaAndSsoLogin() {
		return new HubspotLoginActivity(
				"login-1", "user-1", "alice@example.com",
				"2024-03-01T09:00:00Z", "WEB", "SSO", "SUCCESS",
				true, true, "203.0.113.1", "DE", "BY", "Chrome 122", "DESKTOP" );
	}

	/** A successful login with no MFA (password only). */
	private static HubspotLoginActivity passwordOnlyLogin() {
		return new HubspotLoginActivity(
				"login-2", "user-2", "bob@example.com",
				"2024-03-01T10:00:00Z", "WEB", "PASSWORD", "SUCCESS",
				false, false, "198.51.100.5", "US", "CA", "Firefox 124", "DESKTOP" );
	}

	/** A failed login attempt. */
	private static HubspotLoginActivity failedLogin() {
		return new HubspotLoginActivity(
				"login-3", "user-2", "bob@example.com",
				"2024-03-01T10:01:00Z", "WEB", "PASSWORD", "FAILURE",
				false, false, "198.51.100.99", "RU", null, "curl/7.0", "DESKTOP" );
	}

	/** A mobile app login using two-factor auth but no SSO. */
	private static HubspotLoginActivity mfaOnlyMobileLogin() {
		return new HubspotLoginActivity(
				"login-4", "user-3", "carol@example.com",
				"2024-03-01T11:00:00Z", "MOBILE_APP", "TWO_FACTOR", "SUCCESS",
				false, true, "192.0.2.10", "GB", "ENG", "HubSpot iOS 5.2", "MOBILE" );
	}

	// --- tests ---------------------------------------------------------------

	@Test
	void should_return_all_login_events() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotLoginActivity.class,
					List.of( mfaAndSsoLogin(), passwordOnlyLogin(), failedLogin(), mfaOnlyMobileLogin() ) );

			var result = session.createQuery(
					"SELECT l.id, l.userEmail, l.loginStatus FROM HubspotLoginActivity l",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 4 );
		}
	}

	@Test
	void should_find_logins_without_mfa() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotLoginActivity.class,
					List.of( mfaAndSsoLogin(), passwordOnlyLogin(), failedLogin(), mfaOnlyMobileLogin() ) );

			// Successful logins where MFA was not used — 2FA compliance violation
			var result = session.createQuery(
					"SELECT l.userEmail, l.loginMethod, l.ipAddress FROM HubspotLoginActivity l"
							+ " WHERE l.mfaUsed = false AND l.loginStatus = 'SUCCESS'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "userEmail" ) ).isEqualTo( "bob@example.com" );
		}
	}

	@Test
	void should_find_logins_without_sso() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotLoginActivity.class,
					List.of( mfaAndSsoLogin(), passwordOnlyLogin(), failedLogin(), mfaOnlyMobileLogin() ) );

			// Successful logins that bypassed SSO
			var result = session.createQuery(
					"SELECT l.userEmail, l.loginMethod FROM HubspotLoginActivity l"
							+ " WHERE l.ssoUsed = false AND l.loginStatus = 'SUCCESS'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			// passwordOnlyLogin and mfaOnlyMobileLogin both have ssoUsed = false
			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_failed_login_attempts() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotLoginActivity.class,
					List.of( mfaAndSsoLogin(), passwordOnlyLogin(), failedLogin(), mfaOnlyMobileLogin() ) );

			var result = session.createQuery(
					"SELECT l.userEmail, l.ipAddress, l.countryCode FROM HubspotLoginActivity l"
							+ " WHERE l.loginStatus = 'FAILURE'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "countryCode" ) ).isEqualTo( "RU" );
		}
	}

	@Test
	void should_find_logins_from_unexpected_country() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotLoginActivity.class,
					List.of( mfaAndSsoLogin(), passwordOnlyLogin(), failedLogin(), mfaOnlyMobileLogin() ) );

			// Example: portal allows only DE and GB — flag others
			var result = session.createQuery(
					"SELECT l.userEmail, l.countryCode, l.ipAddress FROM HubspotLoginActivity l"
							+ " WHERE l.countryCode NOT IN ('DE', 'GB', 'US')",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			// RU login (failed) should be flagged
			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "countryCode" ) ).isEqualTo( "RU" );
		}
	}

	@Test
	void should_find_all_successful_logins_with_mfa() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotLoginActivity.class,
					List.of( mfaAndSsoLogin(), passwordOnlyLogin(), failedLogin(), mfaOnlyMobileLogin() ) );

			var result = session.createQuery(
					"SELECT l.userEmail, l.loginMethod FROM HubspotLoginActivity l"
							+ " WHERE l.mfaUsed = true AND l.loginStatus = 'SUCCESS'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}
}
