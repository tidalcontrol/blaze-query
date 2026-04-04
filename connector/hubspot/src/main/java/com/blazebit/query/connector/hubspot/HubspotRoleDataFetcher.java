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
 * Fetches all {@link HubspotRole} records from {@code GET /settings/v3/users/roles}.
 *
 * <p>Roles define which CRM permissions users have. Joining roles with users reveals
 * who has read/write access to contacts, deals, and other CRM objects.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public class HubspotRoleDataFetcher implements DataFetcher<HubspotRole>, Serializable {

	public static final HubspotRoleDataFetcher INSTANCE = new HubspotRoleDataFetcher();

	private HubspotRoleDataFetcher() {
	}

	@Override
	public List<HubspotRole> fetch(DataFetchContext context) {
		try {
			List<HubspotClient> clients = HubspotConnectorConfig.HUBSPOT_CLIENT.getAll( context );
			List<HubspotRole> result = new ArrayList<>();
			for ( HubspotClient client : clients ) {
				result.addAll( client.listRoles() );
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch HubSpot roles", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( HubspotRole.class, HubspotConventionContext.INSTANCE );
	}
}
