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
 * Tests for Scaleway monitoring and email queries — Cockpit AlertManager and TEM Domains.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class ScalewayMonitoringAndEmailTests {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new ScalewaySchemaProvider() );
		builder.registerSchemaObjectAlias( ScalewayCockpitAlertManager.class, "ScalewayCockpitAlertManager" );
		builder.registerSchemaObjectAlias( ScalewayTemDomain.class, "ScalewayTemDomain" );
		CONTEXT = builder.build();
	}

	// -------------------------------------------------------------------------
	// AlertManager test objects
	// -------------------------------------------------------------------------

	private static ScalewayCockpitAlertManager alertManagerWithContacts() {
		return new ScalewayCockpitAlertManager(
				"fr-par",
				"proj-xyz",
				true,
				3
		);
	}

	private static ScalewayCockpitAlertManager alertManagerNoContacts() {
		return new ScalewayCockpitAlertManager(
				"nl-ams",
				"proj-xyz",
				true,
				0
		);
	}

	private static ScalewayCockpitAlertManager alertManagerDisabled() {
		return new ScalewayCockpitAlertManager(
				"pl-waw",
				"proj-xyz",
				false,
				0
		);
	}

	@Test
	void should_return_all_alert_managers() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayCockpitAlertManager.class,
					List.of( alertManagerWithContacts(), alertManagerNoContacts(), alertManagerDisabled() ) );

			var result = session.createQuery(
					"SELECT a.region, a.managedAlertsEnabled, a.contactPointCount FROM ScalewayCockpitAlertManager a",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}

	@Test
	void should_find_alert_managers_without_contacts() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayCockpitAlertManager.class,
					List.of( alertManagerWithContacts(), alertManagerNoContacts(), alertManagerDisabled() ) );

			var result = session.createQuery(
					"SELECT a.region FROM ScalewayCockpitAlertManager a WHERE a.managedAlertsEnabled = true AND a.contactPointCount = 0",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "region" ) ).isEqualTo( "nl-ams" );
		}
	}

	@Test
	void should_find_disabled_alert_managers() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayCockpitAlertManager.class,
					List.of( alertManagerWithContacts(), alertManagerNoContacts(), alertManagerDisabled() ) );

			var result = session.createQuery(
					"SELECT a.region FROM ScalewayCockpitAlertManager a WHERE a.managedAlertsEnabled = false",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "region" ) ).isEqualTo( "pl-waw" );
		}
	}

	// -------------------------------------------------------------------------
	// TEM Domain test objects
	// -------------------------------------------------------------------------

	private static ScalewayTemDomain validDomain() {
		return new ScalewayTemDomain(
				"tem-001",
				"email.mycompany.com",
				"checked",
				"fr-par",
				"proj-xyz",
				"org-abc",
				true,
				true,
				true,
				5000L,
				12L,
				"2024-01-01T00:00:00Z",
				"2026-01-01T00:00:00Z"
		);
	}

	private static ScalewayTemDomain partiallyConfiguredDomain() {
		return new ScalewayTemDomain(
				"tem-002",
				"newsletter.mycompany.com",
				"unchecked",
				"fr-par",
				"proj-xyz",
				"org-abc",
				true,
				false,
				false,
				0L,
				0L,
				"2025-01-01T00:00:00Z",
				"2025-06-01T00:00:00Z"
		);
	}

	private static ScalewayTemDomain revokedDomain() {
		return new ScalewayTemDomain(
				"tem-003",
				"old.mycompany.com",
				"revoked",
				"fr-par",
				"proj-xyz",
				"org-abc",
				true,
				true,
				true,
				1000L,
				100L,
				"2022-01-01T00:00:00Z",
				"2023-01-01T00:00:00Z"
		);
	}

	@Test
	void should_return_all_domains() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayTemDomain.class, List.of( validDomain(), partiallyConfiguredDomain(), revokedDomain() ) );

			var result = session.createQuery(
					"SELECT d.id, d.name, d.status FROM ScalewayTemDomain d",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}

	@Test
	void should_find_fully_configured_domains() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayTemDomain.class, List.of( validDomain(), partiallyConfiguredDomain(), revokedDomain() ) );

			var result = session.createQuery(
					"SELECT d.id, d.name FROM ScalewayTemDomain d WHERE d.spfConfigured = true AND d.dkimConfigured = true AND d.mxConfigured = true",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_unchecked_or_partially_configured_domains() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayTemDomain.class, List.of( validDomain(), partiallyConfiguredDomain(), revokedDomain() ) );

			var result = session.createQuery(
					"SELECT d.id, d.name FROM ScalewayTemDomain d WHERE d.status <> 'checked'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_domains_with_high_failure_rate() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayTemDomain.class, List.of( validDomain(), partiallyConfiguredDomain(), revokedDomain() ) );

			var result = session.createQuery(
					"SELECT d.id, d.name FROM ScalewayTemDomain d WHERE d.totalFailed > 50",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "old.mycompany.com" );
		}
	}
}
