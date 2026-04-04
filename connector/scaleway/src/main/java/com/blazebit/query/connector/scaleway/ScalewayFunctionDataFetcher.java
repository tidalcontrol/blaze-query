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
 * Fetches Scaleway Serverless Functions for compliance auditing:
 * public functions, env-var secret leakage, and runtime version auditing.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public class ScalewayFunctionDataFetcher implements DataFetcher<ScalewayFunction>, Serializable {

	public static final ScalewayFunctionDataFetcher INSTANCE = new ScalewayFunctionDataFetcher();

	private ScalewayFunctionDataFetcher() {
	}

	@Override
	public List<ScalewayFunction> fetch(DataFetchContext context) {
		try {
			List<ScalewayClient> clients = ScalewayConnectorConfig.SCALEWAY_CLIENT.getAll( context );
			List<ScalewayFunction> result = new ArrayList<>();
			for ( ScalewayClient client : clients ) {
				for ( JsonNode node : client.listFunctions() ) {
					result.add( ScalewayFunction.from( node ) );
				}
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Scaleway functions", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( ScalewayFunction.class, ScalewayConventionContext.INSTANCE );
	}
}
