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
 * Fetches Scaleway IAM groups for access control and group membership auditing.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class ScalewayIamGroupDataFetcher implements DataFetcher<ScalewayIamGroup>, Serializable {

	public static final ScalewayIamGroupDataFetcher INSTANCE = new ScalewayIamGroupDataFetcher();

	private ScalewayIamGroupDataFetcher() {
	}

	@Override
	public List<ScalewayIamGroup> fetch(DataFetchContext context) {
		try {
			List<ScalewayClient> clients = ScalewayConnectorConfig.SCALEWAY_CLIENT.getAll( context );
			List<ScalewayIamGroup> result = new ArrayList<>();
			for ( ScalewayClient client : clients ) {
				for ( JsonNode node : client.listIamGroups() ) {
					result.add( ScalewayIamGroup.from( node ) );
				}
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Scaleway IAM groups", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( ScalewayIamGroup.class, ScalewayConventionContext.INSTANCE );
	}
}
