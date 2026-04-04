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
 * Fetches Scaleway Private Networks for network isolation auditing:
 * verifying that workloads are deployed within private networks
 * rather than solely relying on public interfaces.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class ScalewayPrivateNetworkDataFetcher implements DataFetcher<ScalewayPrivateNetwork>, Serializable {

	public static final ScalewayPrivateNetworkDataFetcher INSTANCE = new ScalewayPrivateNetworkDataFetcher();

	private ScalewayPrivateNetworkDataFetcher() {
	}

	@Override
	public List<ScalewayPrivateNetwork> fetch(DataFetchContext context) {
		try {
			List<ScalewayClient> clients = ScalewayConnectorConfig.SCALEWAY_CLIENT.getAll( context );
			List<ScalewayPrivateNetwork> result = new ArrayList<>();
			for ( ScalewayClient client : clients ) {
				for ( JsonNode node : client.listPrivateNetworks() ) {
					result.add( ScalewayPrivateNetwork.from( node ) );
				}
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Scaleway private networks", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( ScalewayPrivateNetwork.class, ScalewayConventionContext.INSTANCE );
	}
}
