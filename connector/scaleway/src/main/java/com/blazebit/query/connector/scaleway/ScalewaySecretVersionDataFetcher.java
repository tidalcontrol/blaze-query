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
 * Fetches all versions of Scaleway secrets for rotation auditing:
 * identifying the age of the latest version to detect secrets that
 * have never been rotated or are overdue for rotation.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public class ScalewaySecretVersionDataFetcher implements DataFetcher<ScalewaySecretVersion>, Serializable {

	public static final ScalewaySecretVersionDataFetcher INSTANCE = new ScalewaySecretVersionDataFetcher();

	private ScalewaySecretVersionDataFetcher() {
	}

	@Override
	public List<ScalewaySecretVersion> fetch(DataFetchContext context) {
		try {
			List<ScalewayClient> clients = ScalewayConnectorConfig.SCALEWAY_CLIENT.getAll( context );
			List<ScalewaySecretVersion> result = new ArrayList<>();
			for ( ScalewayClient client : clients ) {
				List<? extends ScalewaySecret> secrets = context.getSession().getOrFetch( ScalewaySecret.class );
				for ( ScalewaySecret secret : secrets ) {
					if ( secret.id() == null || secret.region() == null ) {
						continue;
					}
					for ( JsonNode node : client.listSecretVersions( secret.id(), secret.region() ) ) {
						result.add( ScalewaySecretVersion.from( node, secret.id(), secret.region() ) );
					}
				}
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Scaleway secret versions", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( ScalewaySecretVersion.class, ScalewayConventionContext.INSTANCE );
	}
}
