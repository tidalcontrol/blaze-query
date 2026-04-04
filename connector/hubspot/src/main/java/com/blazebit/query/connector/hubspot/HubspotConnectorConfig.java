/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.hubspot;

import com.blazebit.query.spi.DataFetcherConfig;

import java.time.Duration;

/**
 * Configuration properties for the HubSpot connector.
 *
 * <p>Example setup:
 * <pre>{@code
 * HubspotClient client = new HubspotClient("your-private-app-access-token");
 * queryContextBuilder.setProperty(HubspotConnectorConfig.HUBSPOT_CLIENT.getPropertyName(), client);
 * }</pre>
 *
 * @author Blazebit
 * @since 2.4.4
 */
public final class HubspotConnectorConfig {

	/**
	 * Specifies the {@link HubspotClient} to use for querying HubSpot data.
	 * The client must be pre-configured with a valid Private App access token.
	 */
	public static final DataFetcherConfig<HubspotClient> HUBSPOT_CLIENT =
			DataFetcherConfig.forPropertyName( "hubspotClient" );

	/**
	 * Optional time window for the audit log fetcher.
	 * Only audit log events newer than {@code now - AUDIT_LOGS_MAX_AGE} are returned.
	 * Defaults to {@link Duration#ofHours(24)} when not set.
	 */
	public static final DataFetcherConfig<Duration> AUDIT_LOGS_MAX_AGE =
			DataFetcherConfig.forPropertyName( "hubspotAuditLogsMaxAge" );

	private HubspotConnectorConfig() {
	}
}
