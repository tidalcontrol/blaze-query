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
 * Fetches all {@link HubspotUser} records from {@code GET /settings/v3/users}.
 *
 * <p>Useful for compliance queries such as:
 * <ul>
 *   <li>Listing users that have access to CRM contact data (join with owners)</li>
 *   <li>Finding inactive / stale users ({@code status = 'INACTIVE'})</li>
 *   <li>Auditing super-admin accounts ({@code superAdmin = true})</li>
 * </ul>
 *
 * @author Blazebit
 * @since 2.4.4
 */
public class HubspotUserDataFetcher implements DataFetcher<HubspotUser>, Serializable {

	public static final HubspotUserDataFetcher INSTANCE = new HubspotUserDataFetcher();

	private HubspotUserDataFetcher() {
	}

	@Override
	public List<HubspotUser> fetch(DataFetchContext context) {
		try {
			List<HubspotClient> clients = HubspotConnectorConfig.HUBSPOT_CLIENT.getAll( context );
			List<HubspotUser> result = new ArrayList<>();
			for ( HubspotClient client : clients ) {
				result.addAll( client.listUsers() );
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch HubSpot users", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( HubspotUser.class, HubspotConventionContext.INSTANCE );
	}
}
