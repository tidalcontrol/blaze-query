/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.hubspot;

import com.blazebit.query.spi.ConfigurationProvider;
import com.blazebit.query.spi.DataFetcher;
import com.blazebit.query.spi.QuerySchemaProvider;

import java.util.Set;

/**
 * Registers all HubSpot data fetchers.
 *
 * <p>Registered fetchers and their primary compliance use-cases:
 * <table>
 *   <tr><th>Fetcher</th><th>Compliance topic</th></tr>
 *   <tr><td>{@link HubspotUserDataFetcher}</td><td>User access, stale accounts, super-admins</td></tr>
 *   <tr><td>{@link HubspotRoleDataFetcher}</td><td>Permission / role definitions</td></tr>
 *   <tr><td>{@link HubspotOwnerDataFetcher}</td><td>Who owns (accesses) CRM contact records</td></tr>
 *   <tr><td>{@link HubspotAccountInfoDataFetcher}</td><td>GDPR data-hosting location, account type</td></tr>
 *   <tr><td>{@link HubspotAuditLogDataFetcher}</td><td>Logging and monitoring (Enterprise only)</td></tr>
 *   <tr><td>{@link HubspotTeamDataFetcher}</td><td>Org structure, separation of duties, orphaned users</td></tr>
 *   <tr><td>{@link HubspotSubscriptionDefinitionDataFetcher}</td><td>GDPR consent categories (default opt-in risk, inactive types)</td></tr>
 *   <tr><td>{@link HubspotLoginActivityDataFetcher}</td><td>2FA / MFA per-login audit (mfaUsed, ssoUsed fields)</td></tr>
 *   <tr><td>{@link HubspotSecurityActivityDataFetcher}</td><td>Security config changes (MFA_ENABLED, SSO_CONFIGURED, …)</td></tr>
 * </table>
 *
 * @author Blazebit
 * @since 2.4.4
 */
public final class HubspotSchemaProvider implements QuerySchemaProvider {

	public HubspotSchemaProvider() {
	}

	@Override
	public Set<? extends DataFetcher<?>> resolveSchemaObjects(ConfigurationProvider configurationProvider) {
		return Set.of(
				HubspotUserDataFetcher.INSTANCE,
				HubspotRoleDataFetcher.INSTANCE,
				HubspotOwnerDataFetcher.INSTANCE,
				HubspotTeamDataFetcher.INSTANCE,
				HubspotAccountInfoDataFetcher.INSTANCE,
				HubspotSubscriptionDefinitionDataFetcher.INSTANCE,
				HubspotAuditLogDataFetcher.INSTANCE,
				HubspotLoginActivityDataFetcher.INSTANCE,
				HubspotSecurityActivityDataFetcher.INSTANCE
		);
	}
}
