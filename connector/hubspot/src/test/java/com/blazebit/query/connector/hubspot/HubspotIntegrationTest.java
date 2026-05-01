/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.hubspot;

import com.blazebit.query.QueryContext;
import com.blazebit.query.TypeReference;
import com.blazebit.query.impl.QueryContextBuilderImpl;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Live integration test — runs the security & compliance queries from CLAUDE.md
 * against a real HubSpot portal. Requires a valid access token set via the
 * {@code HUBSPOT_ACCESS_TOKEN} environment variable.
 *
 * <p>Enterprise-only fetchers (audit logs, login activity, security activity)
 * are skipped gracefully when the portal is non-Enterprise (HTTP 403).
 */
class HubspotIntegrationTest {

	private static QueryContext CONTEXT;

	@BeforeAll
	static void setup() {
		String token = System.getenv( "HUBSPOT_ACCESS_TOKEN" );
		Assumptions.assumeTrue( token != null && !token.isBlank(),
				"HUBSPOT_ACCESS_TOKEN not set — skipping live HubSpot tests" );
		HubspotClient client = new HubspotClient( token );
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new HubspotSchemaProvider() );
		builder.registerSchemaObjectAlias( HubspotUser.class, "HubspotUser" );
		builder.registerSchemaObjectAlias( HubspotRole.class, "HubspotRole" );
		builder.registerSchemaObjectAlias( HubspotOwner.class, "HubspotOwner" );
		builder.registerSchemaObjectAlias( HubspotTeam.class, "HubspotTeam" );
		builder.registerSchemaObjectAlias( HubspotAccountInfo.class, "HubspotAccountInfo" );
		builder.registerSchemaObjectAlias( HubspotSubscriptionDefinition.class, "HubspotSubscriptionDefinition" );
		builder.registerSchemaObjectAlias( HubspotAuditLog.class, "HubspotAuditLog" );
		builder.registerSchemaObjectAlias( HubspotLoginActivity.class, "HubspotLoginActivity" );
		builder.registerSchemaObjectAlias( HubspotSecurityActivity.class, "HubspotSecurityActivity" );
		builder.setProperty( HubspotConnectorConfig.HUBSPOT_CLIENT.getPropertyName(), client );
		CONTEXT = builder.build();
	}

	@Test
	void account_info() {
		try ( var session = CONTEXT.createSession() ) {
			var result = session.createQuery(
					"SELECT portalId, dataHostingLocation, accountType, timeZone, currency FROM HubspotAccountInfo",
					new TypeReference<Map<String, Object>>() {} ).getResultList();
			print( "AccountInfo", result );
		}
	}

	@Test
	void all_users() {
		try ( var session = CONTEXT.createSession() ) {
			var result = session.createQuery(
					"SELECT id, email, status, superAdmin, primaryTeamId FROM HubspotUser",
					new TypeReference<Map<String, Object>>() {} ).getResultList();
			print( "All users", result );
		}
	}

	@Test
	void super_admin_accounts() {
		try ( var session = CONTEXT.createSession() ) {
			var result = session.createQuery(
					"SELECT id, email, primaryTeamId FROM HubspotUser WHERE superAdmin = true",
					new TypeReference<Map<String, Object>>() {} ).getResultList();
			print( "Super-admins", result );
		}
	}

	@Test
	void inactive_users() {
		try ( var session = CONTEXT.createSession() ) {
			var result = session.createQuery(
					"SELECT id, email, status, updatedAt FROM HubspotUser WHERE status = 'INACTIVE'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();
			print( "Inactive users", result );
		}
	}

	@Test
	void all_roles() {
		try ( var session = CONTEXT.createSession() ) {
			var result = session.createQuery(
					"SELECT id, name, requiresBillingWrite FROM HubspotRole",
					new TypeReference<Map<String, Object>>() {} ).getResultList();
			print( "Roles", result );
		}
	}

	@Test
	void all_teams() {
		try ( var session = CONTEXT.createSession() ) {
			var result = session.createQuery(
					"SELECT id, name, parentTeamId FROM HubspotTeam",
					new TypeReference<Map<String, Object>>() {} ).getResultList();
			print( "Teams", result );
		}
	}

	@Test
	void active_crm_owners() {
		try ( var session = CONTEXT.createSession() ) {
			var result = session.createQuery(
					"SELECT id, email, firstName, lastName, userId FROM HubspotOwner WHERE archived = false",
					new TypeReference<Map<String, Object>>() {} ).getResultList();
			print( "Active CRM owners", result );
		}
	}

	@Test
	void users_with_crm_access() {
		try ( var session = CONTEXT.createSession() ) {
			var result = session.createQuery(
					"SELECT u.id, u.email, u.status, u.superAdmin "
							+ "FROM HubspotUser u "
							+ "JOIN HubspotOwner o ON u.email = o.email "
							+ "WHERE o.archived = false AND (u.status = 'ACTIVE' OR u.status IS NULL)",
					new TypeReference<Map<String, Object>>() {} ).getResultList();
			print( "Users with CRM access", result );
		}
	}

	@Test
	void gdpr_data_residency() {
		try ( var session = CONTEXT.createSession() ) {
			var result = session.createQuery(
					"SELECT portalId, dataHostingLocation, accountType FROM HubspotAccountInfo",
					new TypeReference<Map<String, Object>>() {} ).getResultList();
			print( "GDPR data residency", result );
		}
	}

	@Test
	void subscription_definitions() {
		try ( var session = CONTEXT.createSession() ) {
			var result = session.createQuery(
					"SELECT id, name, communicationMethod, active, defaultOptIn FROM HubspotSubscriptionDefinition",
					new TypeReference<Map<String, Object>>() {} ).getResultList();
			print( "Subscription definitions", result );
		}
	}

	@Test
	void gdpr_default_opt_in_risk() {
		try ( var session = CONTEXT.createSession() ) {
			var result = session.createQuery(
					"SELECT id, name, communicationMethod FROM HubspotSubscriptionDefinition WHERE defaultOptIn = true AND active = true",
					new TypeReference<Map<String, Object>>() {} ).getResultList();
			print( "GDPR default opt-in risk", result );
		}
	}

	// --- helpers ----------------------------------------------------------------

	private static void print( String label, List<Map<String, Object>> rows ) {
		System.out.println( "\n=== " + label + " (" + rows.size() + " rows) ===" );
		for ( Map<String, Object> row : rows ) {
			System.out.println( "  " + row );
		}
	}
}
