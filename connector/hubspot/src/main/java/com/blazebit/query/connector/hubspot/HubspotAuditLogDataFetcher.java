/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.hubspot;

import com.blazebit.query.connector.base.DataFormats;
import com.blazebit.query.spi.DataFetchContext;
import com.blazebit.query.spi.DataFetcher;
import com.blazebit.query.spi.DataFetcherException;
import com.blazebit.query.spi.DataFormat;

import java.io.Serializable;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches {@link HubspotAuditLog} events from
 * {@code GET /account-info/v3/activity/audit-logs}.
 *
 * <p>The time window defaults to the last 24 hours and can be customised via
 * {@link HubspotConnectorConfig#AUDIT_LOGS_MAX_AGE}.
 *
 * <p>Requires an Enterprise HubSpot subscription and the
 * {@code account-info.security.read} OAuth scope.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public class HubspotAuditLogDataFetcher implements DataFetcher<HubspotAuditLog>, Serializable {

	public static final HubspotAuditLogDataFetcher INSTANCE = new HubspotAuditLogDataFetcher();

	private static final Duration DEFAULT_MAX_AGE = Duration.ofHours( 24 );

	private HubspotAuditLogDataFetcher() {
	}

	@Override
	public List<HubspotAuditLog> fetch(DataFetchContext context) {
		try {
			List<HubspotClient> clients = HubspotConnectorConfig.HUBSPOT_CLIENT.getAll( context );
			Duration maxAge = HubspotConnectorConfig.AUDIT_LOGS_MAX_AGE.get( context );
			if ( maxAge == null ) {
				maxAge = DEFAULT_MAX_AGE;
			}
			OffsetDateTime from = OffsetDateTime.now( ZoneOffset.UTC ).minus( maxAge );
			List<HubspotAuditLog> result = new ArrayList<>();
			for ( HubspotClient client : clients ) {
				result.addAll( client.listAuditLogs( from, null ) );
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch HubSpot audit logs", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( HubspotAuditLog.class, HubspotConventionContext.INSTANCE );
	}
}
