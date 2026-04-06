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
 * Fetches Scaleway Instance Snapshots for compliance auditing:
 * orphaned snapshots, snapshot age auditing, and storage hygiene.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class ScalewaySnapshotDataFetcher implements DataFetcher<ScalewaySnapshot>, Serializable {

	public static final ScalewaySnapshotDataFetcher INSTANCE = new ScalewaySnapshotDataFetcher();

	private ScalewaySnapshotDataFetcher() {
	}

	@Override
	public List<ScalewaySnapshot> fetch(DataFetchContext context) {
		try {
			List<ScalewayClient> clients = ScalewayConnectorConfig.SCALEWAY_CLIENT.getAll( context );
			List<ScalewaySnapshot> result = new ArrayList<>();
			for ( ScalewayClient client : clients ) {
				for ( JsonNode node : client.listSnapshots() ) {
					result.add( ScalewaySnapshot.from( node ) );
				}
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Scaleway snapshots", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( ScalewaySnapshot.class, ScalewayConventionContext.INSTANCE );
	}
}
