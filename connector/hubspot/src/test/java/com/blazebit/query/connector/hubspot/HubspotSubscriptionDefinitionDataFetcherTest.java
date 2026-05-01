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
 * Tests for {@link HubspotSubscriptionDefinitionDataFetcher} covering GDPR
 * consent category compliance:
 * <ul>
 *   <li>Active consent types exist</li>
 *   <li>Default opt-in types (implicit consent risk)</li>
 *   <li>Hidden internal-only types</li>
 *   <li>Inactive types that should be cleaned up</li>
 * </ul>
 */
class HubspotSubscriptionDefinitionDataFetcherTest {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new HubspotSchemaProvider() );
		builder.registerSchemaObjectAlias( HubspotSubscriptionDefinition.class, "HubspotSubscriptionDefinition" );
		CONTEXT = builder.build();
	}

	// --- test data -----------------------------------------------------------

	/** Standard active marketing consent type — GDPR-safe (not default). */
	private static HubspotSubscriptionDefinition marketingEmails() {
		return new HubspotSubscriptionDefinition(
				"sub-1", "Marketing Emails", "Promotional content and newsletters",
				true, false, false, "EMAIL", "MARKETING",
				"2023-01-01T00:00:00Z", "2024-01-01T00:00:00Z", null );
	}

	/** Transactional type — acceptable as default because it's non-promotional. */
	private static HubspotSubscriptionDefinition transactional() {
		return new HubspotSubscriptionDefinition(
				"sub-2", "Order Confirmations", "Transactional order and receipt emails",
				true, true, false, "EMAIL", "TRANSACTIONAL",
				"2023-01-01T00:00:00Z", "2024-01-01T00:00:00Z", null );
	}

	/** Default marketing type — implicit consent risk under GDPR. */
	private static HubspotSubscriptionDefinition defaultMarketing() {
		return new HubspotSubscriptionDefinition(
				"sub-3", "Weekly Digest", "Default-subscribed marketing digest",
				true, true, false, "EMAIL", "MARKETING",
				"2022-06-01T00:00:00Z", "2023-03-01T00:00:00Z", null );
	}

	/** Internal type hidden from contacts — should be reviewed. */
	private static HubspotSubscriptionDefinition internalType() {
		return new HubspotSubscriptionDefinition(
				"sub-4", "Internal Alerts", "System-generated internal notifications",
				true, false, true, "EMAIL", "TRANSACTIONAL",
				"2023-05-01T00:00:00Z", "2023-05-01T00:00:00Z", null );
	}

	/** Inactive type — should be cleaned up or archived. */
	private static HubspotSubscriptionDefinition inactiveType() {
		return new HubspotSubscriptionDefinition(
				"sub-5", "Old Newsletter", "Discontinued newsletter",
				false, false, false, "EMAIL", "MARKETING",
				"2021-01-01T00:00:00Z", "2022-12-01T00:00:00Z", null );
	}

	// --- tests ---------------------------------------------------------------

	@Test
	void should_return_all_subscription_definitions() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotSubscriptionDefinition.class,
					List.of( marketingEmails(), transactional(), defaultMarketing(),
							internalType(), inactiveType() ) );

			var result = session.createQuery(
					"SELECT s.id, s.name, s.active FROM HubspotSubscriptionDefinition s",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 5 );
		}
	}

	@Test
	void should_find_active_consent_types() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotSubscriptionDefinition.class,
					List.of( marketingEmails(), transactional(), defaultMarketing(),
							internalType(), inactiveType() ) );

			var result = session.createQuery(
					"SELECT s.id, s.name, s.purpose FROM HubspotSubscriptionDefinition s"
							+ " WHERE s.active = true",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 4 );
		}
	}

	@Test
	void should_find_default_opt_in_marketing_types() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotSubscriptionDefinition.class,
					List.of( marketingEmails(), transactional(), defaultMarketing(),
							internalType(), inactiveType() ) );

			// Default marketing subscriptions silently opt-in contacts — GDPR implicit consent risk.
			var result = session.createQuery(
					"SELECT s.id, s.name, s.purpose FROM HubspotSubscriptionDefinition s"
							+ " WHERE s.defaultOptIn = true AND s.purpose = 'MARKETING'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "Weekly Digest" );
		}
	}

	@Test
	void should_find_internal_hidden_types() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotSubscriptionDefinition.class,
					List.of( marketingEmails(), transactional(), defaultMarketing(),
							internalType(), inactiveType() ) );

			// Internal types are hidden from contacts — confirm their use is intentional.
			var result = session.createQuery(
					"SELECT s.id, s.name, s.purpose FROM HubspotSubscriptionDefinition s"
							+ " WHERE s.internal = true",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "Internal Alerts" );
		}
	}

	@Test
	void should_find_inactive_types_for_cleanup() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotSubscriptionDefinition.class,
					List.of( marketingEmails(), transactional(), defaultMarketing(),
							internalType(), inactiveType() ) );

			var result = session.createQuery(
					"SELECT s.id, s.name FROM HubspotSubscriptionDefinition s"
							+ " WHERE s.active = false",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "Old Newsletter" );
		}
	}

	@Test
	void should_verify_no_default_marketing_subscription_exists() {
		try (var session = CONTEXT.createSession()) {
			// Ideal GDPR posture: no marketing subscription type is set as default.
			session.put( HubspotSubscriptionDefinition.class,
					List.of( marketingEmails(), transactional() ) );

			var result = session.createQuery(
					"SELECT s.id, s.name FROM HubspotSubscriptionDefinition s"
							+ " WHERE s.defaultOptIn = true AND s.purpose = 'MARKETING'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			// Green: no implicit marketing opt-in configured
			assertThat( result ).isEmpty();
		}
	}
}
