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
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches {@link HubspotLoginActivity} events from
 * {@code GET /account-info/v3/activity/login}.
 *
 * <p>The endpoint does not support time-range filtering — every call returns
 * the full set of login events available to the portal, paginated.
 *
 * <p>Requires an Enterprise HubSpot subscription and the
 * {@code account-info.security.read} scope.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class HubspotLoginActivityDataFetcher implements DataFetcher<HubspotLoginActivity>, Serializable {

	public static final HubspotLoginActivityDataFetcher INSTANCE = new HubspotLoginActivityDataFetcher();

	private HubspotLoginActivityDataFetcher() {
	}

	@Override
	public List<HubspotLoginActivity> fetch(DataFetchContext context) {
		try {
			List<HubspotClient> clients = HubspotConnectorConfig.HUBSPOT_CLIENT.getAll( context );
			List<HubspotLoginActivity> result = new ArrayList<>();
			for ( HubspotClient client : clients ) {
				result.addAll( client.listLoginActivity( null ) );
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
