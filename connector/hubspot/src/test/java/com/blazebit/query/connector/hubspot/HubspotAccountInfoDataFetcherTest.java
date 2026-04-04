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
 * Tests for {@link HubspotAccountInfoDataFetcher} covering the compliance query
 * "GDPR governance module on".
 *
 * <p>The {@code dataHostingLocation} field signals EU data residency ({@code eu1}),
 * which is the primary infrastructure indicator for GDPR compliance in HubSpot.
 */
class HubspotAccountInfoDataFetcherTest {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new HubspotSchemaProvider() );
		builder.registerSchemaObjectAlias( HubspotAccountInfo.class, "HubspotAccountInfo" );
		CONTEXT = builder.build();
	}

	// --- test data -----------------------------------------------------------

	private static HubspotAccountInfo euPortal() {
		return new HubspotAccountInfo(
				12345L,
				"Europe/Berlin",
				"EUR",
				"+01:00",
				3600000L,
				"app.hubspot.com",
				"eu1",
				"STANDARD"
		);
	}

	private static HubspotAccountInfo usPortal() {
		return new HubspotAccountInfo(
				67890L,
				"US/Eastern",
				"USD",
				"-05:00",
				-18000000L,
				"app.hubspot.com",
				"na1",
				"STANDARD"
		);
	}

	private static HubspotAccountInfo sandboxPortal() {
		return new HubspotAccountInfo(
				99999L,
				"US/Eastern",
				"USD",
				"-05:00",
				-18000000L,
				"app.hubspot.com",
				"na1",
				"SANDBOX"
		);
	}

	// --- tests ---------------------------------------------------------------

	@Test
	void should_return_account_info() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotAccountInfo.class, List.of( euPortal() ) );

			var result = session.createQuery(
					"SELECT a.portalId, a.dataHostingLocation, a.accountType FROM HubspotAccountInfo a",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "dataHostingLocation" ) ).isEqualTo( "eu1" );
		}
	}

	@Test
	void should_find_eu_hosted_portals_for_gdpr_compliance() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotAccountInfo.class, List.of( euPortal(), usPortal() ) );

			// EU data hosting is required for GDPR data-residency compliance
			var result = session.createQuery(
					"SELECT a.portalId, a.dataHostingLocation FROM HubspotAccountInfo a"
							+ " WHERE a.dataHostingLocation = 'eu1'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "portalId" ) ).isEqualTo( 12345L );
		}
	}

	@Test
	void should_detect_non_eu_portals_as_gdpr_risk() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotAccountInfo.class, List.of( euPortal(), usPortal() ) );

			var result = session.createQuery(
					"SELECT a.portalId, a.dataHostingLocation FROM HubspotAccountInfo a"
							+ " WHERE a.dataHostingLocation <> 'eu1'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "dataHostingLocation" ) ).isEqualTo( "na1" );
		}
	}

	@Test
	void should_identify_production_portals() {
		try (var session = CONTEXT.createSession()) {
			session.put( HubspotAccountInfo.class, List.of( euPortal(), usPortal(), sandboxPortal() ) );

			var result = session.createQuery(
					"SELECT a.portalId, a.accountType FROM HubspotAccountInfo a"
							+ " WHERE a.accountType = 'STANDARD'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}
}
