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
 * Fetches Scaleway Container Registry images for compliance auditing:
 * publicly visible images and untagged (dangling) images.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public class ScalewayRegistryImageDataFetcher implements DataFetcher<ScalewayRegistryImage>, Serializable {

	public static final ScalewayRegistryImageDataFetcher INSTANCE = new ScalewayRegistryImageDataFetcher();

	private ScalewayRegistryImageDataFetcher() {
	}

	@Override
	public List<ScalewayRegistryImage> fetch(DataFetchContext context) {
		try {
			List<ScalewayClient> clients = ScalewayConnectorConfig.SCALEWAY_CLIENT.getAll( context );
			List<ScalewayRegistryImage> result = new ArrayList<>();
			for ( ScalewayClient client : clients ) {
				for ( JsonNode node : client.listRegistryImages() ) {
					result.add( ScalewayRegistryImage.from( node ) );
				}
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Scaleway registry images", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( ScalewayRegistryImage.class, ScalewayConventionContext.INSTANCE );
	}
}
