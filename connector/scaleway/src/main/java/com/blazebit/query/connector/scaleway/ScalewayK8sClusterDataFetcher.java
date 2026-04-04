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
 * Fetches Scaleway Kubernetes Kapsule clusters for compliance auditing:
 * outdated Kubernetes versions, pending upgrades, and clusters not
 * isolated within a private network.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public class ScalewayK8sClusterDataFetcher implements DataFetcher<ScalewayK8sCluster>, Serializable {

	public static final ScalewayK8sClusterDataFetcher INSTANCE = new ScalewayK8sClusterDataFetcher();

	private ScalewayK8sClusterDataFetcher() {
	}

	@Override
	public List<ScalewayK8sCluster> fetch(DataFetchContext context) {
		try {
			List<ScalewayClient> clients = ScalewayConnectorConfig.SCALEWAY_CLIENT.getAll( context );
			List<ScalewayK8sCluster> result = new ArrayList<>();
			for ( ScalewayClient client : clients ) {
				for ( JsonNode node : client.listK8sClusters() ) {
					result.add( ScalewayK8sCluster.from( node ) );
				}
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Scaleway Kubernetes clusters", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( ScalewayK8sCluster.class, ScalewayConventionContext.INSTANCE );
	}
}
