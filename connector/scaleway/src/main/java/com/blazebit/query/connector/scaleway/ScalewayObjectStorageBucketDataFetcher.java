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
 * Fetches Scaleway Object Storage buckets for security and compliance auditing:
 * public bucket exposure, orphaned/empty buckets, and data volume hygiene.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class ScalewayObjectStorageBucketDataFetcher implements DataFetcher<ScalewayObjectStorageBucket>, Serializable {

	public static final ScalewayObjectStorageBucketDataFetcher INSTANCE = new ScalewayObjectStorageBucketDataFetcher();

	private ScalewayObjectStorageBucketDataFetcher() {
	}

	@Override
	public List<ScalewayObjectStorageBucket> fetch(DataFetchContext context) {
		try {
			List<ScalewayClient> clients = ScalewayConnectorConfig.SCALEWAY_CLIENT.getAll( context );
			List<ScalewayObjectStorageBucket> result = new ArrayList<>();
			for ( ScalewayClient client : clients ) {
				for ( JsonNode node : client.listObjectStorageBuckets() ) {
					result.add( ScalewayObjectStorageBucket.from( node ) );
				}
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Scaleway Object Storage buckets", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( ScalewayObjectStorageBucket.class, ScalewayConventionContext.INSTANCE );
	}
}
