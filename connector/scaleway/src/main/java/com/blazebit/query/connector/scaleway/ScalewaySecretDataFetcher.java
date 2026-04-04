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
 * Fetches Scaleway Secret Manager secrets for compliance auditing:
 * disabled secrets, secrets with zero versions, and stale secrets by project.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class ScalewaySecretDataFetcher implements DataFetcher<ScalewaySecret>, Serializable {

	public static final ScalewaySecretDataFetcher INSTANCE = new ScalewaySecretDataFetcher();

	private ScalewaySecretDataFetcher() {
	}

	@Override
	public List<ScalewaySecret> fetch(DataFetchContext context) {
		try {
			List<ScalewayClient> clients = ScalewayConnectorConfig.SCALEWAY_CLIENT.getAll( context );
			List<ScalewaySecret> result = new ArrayList<>();
			for ( ScalewayClient client : clients ) {
				for ( JsonNode node : client.listSecrets() ) {
					result.add( ScalewaySecret.from( node ) );
				}
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Scaleway secrets", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( ScalewaySecret.class, ScalewayConventionContext.INSTANCE );
	}
}
