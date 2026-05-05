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
 * Tests for {@link HubspotLoginActivityDataFetcher} covering the failed-login
 * and unexpected-country compliance queries supported by the
 * {@code PublicLoginAudit} schema.
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

	private static HubspotLoginActivity successfulDesktopLogin() {
		return new HubspotLoginActivity(
				"login-1", "2024-03-01T09:00:00Z", true, 1001, "alice@example.com",
				"203.0.113.1", "Mozilla/5.0 (Macintosh) Chrome/122.0", "Munich, Germany",
				"DE", "BY" );
	}

	private static HubspotLoginActivity successfulMobileLogin() {
		return new HubspotLoginActivity(
				"login-2", "2024-03-01T10:00:00Z", true, 1002, "bob@example.com",
				"198.51.100.5", "HubSpot iOS 5.2", "San Francisco, United States",
				"US", "CA" );
	}

	private static HubspotLoginActivity failedLogin() {
		return new HubspotLoginActivity(
				"login-3", "2024-03-01T10:01:00Z", false, 1002, "bob@example.com",
				"198.51.100.99", "curl/7.0", "Moscow, Russia",
				"RU", null );
	}

	private static HubspotLoginActivity successfulLondonLogin() {
		return new HubspotLoginActivity(
				"login-4", "2024-03-01T11:00:00Z", true, 1003, "carol@example.com",
				"192.0.2.10", "Mozilla/5.0 (Windows) Firefox/124.0", "London, United Kingdom",
				"GB", "ENG" );
	}

	// --- tests ---------------------------------------------------------------

	@Test
	void should_return_all_login_events() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotLoginActivity.class,
					List.of( successfulDesktopLogin(), successfulMobileLogin(), failedLogin(), successfulLondonLogin() ) );

			var result = session.createQuery(
					"SELECT l.id, l.email, l.loginSucceeded FROM HubspotLoginActivity l",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 4 );
		}
	}

	@Test
	void should_find_failed_login_attempts() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotLoginActivity.class,
					List.of( successfulDesktopLogin(), successfulMobileLogin(), failedLogin(), successfulLondonLogin() ) );

			var result = session.createQuery(
					"SELECT l.email, l.ipAddress, l.countryCode FROM HubspotLoginActivity l"
							+ " WHERE l.loginSucceeded = false",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "countryCode" ) ).isEqualTo( "RU" );
		}
	}

	@Test
	void should_find_logins_from_unexpected_country() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotLoginActivity.class,
					List.of( successfulDesktopLogin(), successfulMobileLogin(), failedLogin(), successfulLondonLogin() ) );

			var result = session.createQuery(
					"SELECT l.email, l.countryCode, l.ipAddress FROM HubspotLoginActivity l"
							+ " WHERE l.countryCode NOT IN ('DE', 'GB', 'US')",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "countryCode" ) ).isEqualTo( "RU" );
		}
	}

	@Test
	void should_filter_logins_by_user() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotLoginActivity.class,
					List.of( successfulDesktopLogin(), successfulMobileLogin(), failedLogin(), successfulLondonLogin() ) );

			var result = session.createQuery(
					"SELECT l.id, l.loginSucceeded FROM HubspotLoginActivity l WHERE l.userId = 1002",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}
}
