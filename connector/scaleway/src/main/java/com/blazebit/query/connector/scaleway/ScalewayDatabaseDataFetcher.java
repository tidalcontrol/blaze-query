/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.blazebit.query.connector.base.DataFormats;
import com.blazebit.query.spi.DataFetchContext;
import com.blazebit.query.spi.DataFetcher;
import com.blazebit.query.spi.DataFetcherException;
import com.blazebit.query.spi.DataFormat;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches Scaleway Managed Database instances for compliance auditing:
 * public endpoints, missing HA, and insufficient backup retention.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public class ScalewayDatabaseDataFetcher implements DataFetcher<ScalewayDatabase>, Serializable {

	public static final ScalewayDatabaseDataFetcher INSTANCE = new ScalewayDatabaseDataFetcher();

	private ScalewayDatabaseDataFetcher() {
	}

	@Override
	public List<ScalewayDatabase> fetch(DataFetchContext context) {
		try {
			List<ScalewayClient> clients = ScalewayConnectorConfig.SCALEWAY_CLIENT.getAll( context );
			List<ScalewayDatabase> result = new ArrayList<>();
			for ( ScalewayClient client : clients ) {
				for ( JsonNode node : client.listDatabases() ) {
					result.add( ScalewayDatabase.from( node ) );
				}
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Scaleway databases", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( ScalewayDatabase.class, ScalewayConventionContext.INSTANCE );
	}
}
