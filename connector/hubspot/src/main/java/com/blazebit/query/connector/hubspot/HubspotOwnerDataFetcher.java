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
 * Fetches all active (non-archived) {@link HubspotOwner} records from
 * {@code GET /crm/v3/owners}.
 *
 * <p>CRM owners are the users that hold ownership of contacts, deals, and other CRM
 * records. Querying this table identifies everyone with effective access to contact data.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class HubspotOwnerDataFetcher implements DataFetcher<HubspotOwner>, Serializable {

	public static final HubspotOwnerDataFetcher INSTANCE = new HubspotOwnerDataFetcher();

	private HubspotOwnerDataFetcher() {
	}

	@Override
	public List<HubspotOwner> fetch(DataFetchContext context) {
		try {
			List<HubspotClient> clients = HubspotConnectorConfig.HUBSPOT_CLIENT.getAll( context );
			List<HubspotOwner> result = new ArrayList<>();
			for ( HubspotClient client : clients ) {
				result.addAll( client.listOwners() );
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch HubSpot owners", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( HubspotOwner.class, HubspotConventionContext.INSTANCE );
	}
}
