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
 * Fetches Scaleway IAM users for security and compliance queries.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class ScalewayIamUserDataFetcher implements DataFetcher<ScalewayIamUser>, Serializable {

	public static final ScalewayIamUserDataFetcher INSTANCE = new ScalewayIamUserDataFetcher();

	private ScalewayIamUserDataFetcher() {
	}

	@Override
	public List<ScalewayIamUser> fetch(DataFetchContext context) {
		try {
			List<ScalewayClient> clients = ScalewayConnectorConfig.SCALEWAY_CLIENT.getAll( context );
			List<ScalewayIamUser> result = new ArrayList<>();
			for ( ScalewayClient client : clients ) {
				for ( JsonNode node : client.listIamUsers() ) {
					result.add( ScalewayIamUser.from( node ) );
				}
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Scaleway IAM users", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( ScalewayIamUser.class, ScalewayConventionContext.INSTANCE );
	}
}
