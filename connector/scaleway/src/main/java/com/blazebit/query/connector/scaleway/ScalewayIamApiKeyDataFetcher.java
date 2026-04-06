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
 * Fetches Scaleway IAM API keys for credential hygiene auditing.
 * Supports detection of keys without expiry, expired keys, and orphaned keys.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class ScalewayIamApiKeyDataFetcher implements DataFetcher<ScalewayIamApiKey>, Serializable {

	public static final ScalewayIamApiKeyDataFetcher INSTANCE = new ScalewayIamApiKeyDataFetcher();

	private ScalewayIamApiKeyDataFetcher() {
	}

	@Override
	public List<ScalewayIamApiKey> fetch(DataFetchContext context) {
		try {
			List<ScalewayClient> clients = ScalewayConnectorConfig.SCALEWAY_CLIENT.getAll( context );
			List<ScalewayIamApiKey> result = new ArrayList<>();
			for ( ScalewayClient client : clients ) {
				for ( JsonNode node : client.listIamApiKeys() ) {
					result.add( ScalewayIamApiKey.from( node ) );
				}
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Scaleway IAM API keys", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( ScalewayIamApiKey.class, ScalewayConventionContext.INSTANCE );
	}
}
