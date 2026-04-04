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
 * Fetches all {@link HubspotTeam} records from {@code GET /settings/v3/users/teams}.
 *
 * <p>Teams define the organisational access structure for CRM records. Use team
 * data to audit separation of duties, identify orphaned users, and map record
 * visibility to organisational boundaries.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public class HubspotTeamDataFetcher implements DataFetcher<HubspotTeam>, Serializable {

	public static final HubspotTeamDataFetcher INSTANCE = new HubspotTeamDataFetcher();

	private HubspotTeamDataFetcher() {
	}

	@Override
	public List<HubspotTeam> fetch(DataFetchContext context) {
		try {
			List<HubspotClient> clients = HubspotConnectorConfig.HUBSPOT_CLIENT.getAll( context );
			List<HubspotTeam> result = new ArrayList<>();
			for ( HubspotClient client : clients ) {
				result.addAll( client.listTeams() );
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch HubSpot teams", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( HubspotTeam.class, HubspotConventionContext.INSTANCE );
	}
}
