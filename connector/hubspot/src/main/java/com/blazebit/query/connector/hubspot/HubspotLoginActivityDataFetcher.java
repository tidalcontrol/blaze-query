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
 * Fetches {@link HubspotLoginActivity} events from
 * {@code GET /account-info/v3/activity/login}.
 *
 * <p>Login events include {@link HubspotLoginActivity#mfaUsed()} and
 * {@link HubspotLoginActivity#ssoUsed()} fields, making this the primary
 * source for 2FA compliance auditing.
 *
 * <p>The time window defaults to the last 24 hours and can be customised via
 * {@link HubspotConnectorConfig#AUDIT_LOGS_MAX_AGE}.
 *
 * <p>Requires an Enterprise HubSpot subscription and the
 * {@code account-info.security.read} scope.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class HubspotLoginActivityDataFetcher implements DataFetcher<HubspotLoginActivity>, Serializable {

	public static final HubspotLoginActivityDataFetcher INSTANCE = new HubspotLoginActivityDataFetcher();

	private static final Duration DEFAULT_MAX_AGE = Duration.ofHours( 24 );

	private HubspotLoginActivityDataFetcher() {
	}

	@Override
	public List<HubspotLoginActivity> fetch(DataFetchContext context) {
		try {
			List<HubspotClient> clients = HubspotConnectorConfig.HUBSPOT_CLIENT.getAll( context );
			Duration maxAge = HubspotConnectorConfig.AUDIT_LOGS_MAX_AGE.get( context );
			if ( maxAge == null ) {
				maxAge = DEFAULT_MAX_AGE;
			}
			OffsetDateTime from = OffsetDateTime.now( ZoneOffset.UTC ).minus( maxAge );
			List<HubspotLoginActivity> result = new ArrayList<>();
			for ( HubspotClient client : clients ) {
				result.addAll( client.listLoginActivity( from, null ) );
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch HubSpot login activity", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( HubspotLoginActivity.class, HubspotConventionContext.INSTANCE );
	}
}
