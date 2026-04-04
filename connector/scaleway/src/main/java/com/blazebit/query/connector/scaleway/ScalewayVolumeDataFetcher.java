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
 * Fetches Scaleway Block Storage Volumes for compliance auditing:
 * orphaned/unattached volumes, volume type auditing, and storage hygiene.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public class ScalewayVolumeDataFetcher implements DataFetcher<ScalewayVolume>, Serializable {

	public static final ScalewayVolumeDataFetcher INSTANCE = new ScalewayVolumeDataFetcher();

	private ScalewayVolumeDataFetcher() {
	}

	@Override
	public List<ScalewayVolume> fetch(DataFetchContext context) {
		try {
			List<ScalewayClient> clients = ScalewayConnectorConfig.SCALEWAY_CLIENT.getAll( context );
			List<ScalewayVolume> result = new ArrayList<>();
			for ( ScalewayClient client : clients ) {
				for ( JsonNode node : client.listVolumes() ) {
					result.add( ScalewayVolume.from( node ) );
				}
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Scaleway volumes", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( ScalewayVolume.class, ScalewayConventionContext.INSTANCE );
	}
}
