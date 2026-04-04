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
 * Fetches Scaleway Serverless Containers for compliance auditing:
 * public containers, env-var secret leakage, and scaling configuration.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public class ScalewayContainerDataFetcher implements DataFetcher<ScalewayContainer>, Serializable {

	public static final ScalewayContainerDataFetcher INSTANCE = new ScalewayContainerDataFetcher();

	private ScalewayContainerDataFetcher() {
	}

	@Override
	public List<ScalewayContainer> fetch(DataFetchContext context) {
		try {
			List<ScalewayClient> clients = ScalewayConnectorConfig.SCALEWAY_CLIENT.getAll( context );
			List<ScalewayContainer> result = new ArrayList<>();
			for ( ScalewayClient client : clients ) {
				for ( JsonNode node : client.listContainers() ) {
					result.add( ScalewayContainer.from( node ) );
				}
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Scaleway containers", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( ScalewayContainer.class, ScalewayConventionContext.INSTANCE );
	}
}
