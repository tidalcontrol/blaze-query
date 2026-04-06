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
 * Fetches Scaleway Compute Instances for infrastructure auditing:
 * running state, public IP exposure, security group assignment, and deletion protection.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class ScalewayInstanceDataFetcher implements DataFetcher<ScalewayInstance>, Serializable {

	public static final ScalewayInstanceDataFetcher INSTANCE = new ScalewayInstanceDataFetcher();

	private ScalewayInstanceDataFetcher() {
	}

	@Override
	public List<ScalewayInstance> fetch(DataFetchContext context) {
		try {
			List<ScalewayClient> clients = ScalewayConnectorConfig.SCALEWAY_CLIENT.getAll( context );
			List<ScalewayInstance> result = new ArrayList<>();
			for ( ScalewayClient client : clients ) {
				for ( JsonNode node : client.listInstances() ) {
					result.add( ScalewayInstance.from( node ) );
				}
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Scaleway instances", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( ScalewayInstance.class, ScalewayConventionContext.INSTANCE );
	}
}
