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
 * Fetches Scaleway SSH keys for credential hygiene and access auditing.
 * Supports detection of disabled keys and stale key assignments.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class ScalewayIamSshKeyDataFetcher implements DataFetcher<ScalewayIamSshKey>, Serializable {

	public static final ScalewayIamSshKeyDataFetcher INSTANCE = new ScalewayIamSshKeyDataFetcher();

	private ScalewayIamSshKeyDataFetcher() {
	}

	@Override
	public List<ScalewayIamSshKey> fetch(DataFetchContext context) {
		try {
			List<ScalewayClient> clients = ScalewayConnectorConfig.SCALEWAY_CLIENT.getAll( context );
			List<ScalewayIamSshKey> result = new ArrayList<>();
			for ( ScalewayClient client : clients ) {
				for ( JsonNode node : client.listIamSshKeys() ) {
					result.add( ScalewayIamSshKey.from( node ) );
				}
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Scaleway SSH keys", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( ScalewayIamSshKey.class, ScalewayConventionContext.INSTANCE );
	}
}
