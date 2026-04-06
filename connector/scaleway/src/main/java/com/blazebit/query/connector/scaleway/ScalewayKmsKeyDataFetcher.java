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
 * Fetches Scaleway KMS keys for encryption compliance auditing:
 * keys without a rotation policy, disabled/locked keys, and overdue rotations.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class ScalewayKmsKeyDataFetcher implements DataFetcher<ScalewayKmsKey>, Serializable {

	public static final ScalewayKmsKeyDataFetcher INSTANCE = new ScalewayKmsKeyDataFetcher();

	private ScalewayKmsKeyDataFetcher() {
	}

	@Override
	public List<ScalewayKmsKey> fetch(DataFetchContext context) {
		try {
			List<ScalewayClient> clients = ScalewayConnectorConfig.SCALEWAY_CLIENT.getAll( context );
			List<ScalewayKmsKey> result = new ArrayList<>();
			for ( ScalewayClient client : clients ) {
				for ( JsonNode node : client.listKmsKeys() ) {
					result.add( ScalewayKmsKey.from( node ) );
				}
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Scaleway KMS keys", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( ScalewayKmsKey.class, ScalewayConventionContext.INSTANCE );
	}
}
