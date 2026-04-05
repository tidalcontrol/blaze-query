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
 * Fetches portal-level account information from {@code GET /account-info/v3/details}.
 *
 * <p>Returns one row per configured portal. The {@link HubspotAccountInfo#dataHostingLocation()}
 * field indicates whether data is stored in the EU ({@code eu1}) or North America ({@code na1}),
 * which is a key indicator for GDPR data-residency compliance.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class HubspotAccountInfoDataFetcher implements DataFetcher<HubspotAccountInfo>, Serializable {

	public static final HubspotAccountInfoDataFetcher INSTANCE = new HubspotAccountInfoDataFetcher();

	private HubspotAccountInfoDataFetcher() {
	}

	@Override
	public List<HubspotAccountInfo> fetch(DataFetchContext context) {
		try {
			List<HubspotClient> clients = HubspotConnectorConfig.HUBSPOT_CLIENT.getAll( context );
			List<HubspotAccountInfo> result = new ArrayList<>();
			for ( HubspotClient client : clients ) {
				result.add( client.getAccountInfo() );
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch HubSpot account info", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( HubspotAccountInfo.class, HubspotConventionContext.INSTANCE );
	}
}
