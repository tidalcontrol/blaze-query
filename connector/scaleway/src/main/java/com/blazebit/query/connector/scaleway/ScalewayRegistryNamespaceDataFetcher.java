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
 * Fetches Scaleway Container Registry namespaces for compliance auditing:
 * publicly accessible registries and namespaces with no images.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public class ScalewayRegistryNamespaceDataFetcher implements DataFetcher<ScalewayRegistryNamespace>, Serializable {

	public static final ScalewayRegistryNamespaceDataFetcher INSTANCE = new ScalewayRegistryNamespaceDataFetcher();

	private ScalewayRegistryNamespaceDataFetcher() {
	}

	@Override
	public List<ScalewayRegistryNamespace> fetch(DataFetchContext context) {
		try {
			List<ScalewayClient> clients = ScalewayConnectorConfig.SCALEWAY_CLIENT.getAll( context );
			List<ScalewayRegistryNamespace> result = new ArrayList<>();
			for ( ScalewayClient client : clients ) {
				for ( JsonNode node : client.listRegistryNamespaces() ) {
					result.add( ScalewayRegistryNamespace.from( node ) );
				}
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Scaleway registry namespaces", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( ScalewayRegistryNamespace.class, ScalewayConventionContext.INSTANCE );
	}
}
