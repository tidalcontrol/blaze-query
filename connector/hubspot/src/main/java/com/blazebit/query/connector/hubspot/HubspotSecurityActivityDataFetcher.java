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
 * Fetches {@link HubspotSecurityActivity} events from
 * {@code GET /account-info/v3/activity/security}.
 *
 * <p>Security activity tracks changes to the portal's security posture: toggling
 * MFA or SSO, modifying user permissions, rotating API tokens, and similar
 * high-impact configuration operations. Use this for change-management auditing
 * and detecting unauthorized security configuration modifications.
 *
 * <p>The time window defaults to the last 24 hours and can be customised via
 * {@link HubspotConnectorConfig#AUDIT_LOGS_MAX_AGE}.
 *
 * <p>Requires an Enterprise HubSpot subscription and the
 * {@code account-info.security.read} scope.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public class HubspotSecurityActivityDataFetcher implements DataFetcher<HubspotSecurityActivity>, Serializable {

	public static final HubspotSecurityActivityDataFetcher INSTANCE = new HubspotSecurityActivityDataFetcher();

	private static final Duration DEFAULT_MAX_AGE = Duration.ofHours( 24 );

	private HubspotSecurityActivityDataFetcher() {
	}

	@Override
	public List<HubspotSecurityActivity> fetch(DataFetchContext context) {
		try {
			List<HubspotClient> clients = HubspotConnectorConfig.HUBSPOT_CLIENT.getAll( context );
			Duration maxAge = HubspotConnectorConfig.AUDIT_LOGS_MAX_AGE.get( context );
			if ( maxAge == null ) {
				maxAge = DEFAULT_MAX_AGE;
			}
			OffsetDateTime from = OffsetDateTime.now( ZoneOffset.UTC ).minus( maxAge );
			List<HubspotSecurityActivity> result = new ArrayList<>();
			for ( HubspotClient client : clients ) {
				result.addAll( client.listSecurityActivity( from, null ) );
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch HubSpot security activity", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( HubspotSecurityActivity.class, HubspotConventionContext.INSTANCE );
	}
}
