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
 * Fetches Scaleway IAM applications (service principals) for service account auditing.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public class ScalewayIamApplicationDataFetcher implements DataFetcher<ScalewayIamApplication>, Serializable {

	public static final ScalewayIamApplicationDataFetcher INSTANCE = new ScalewayIamApplicationDataFetcher();

	private ScalewayIamApplicationDataFetcher() {
	}

	@Override
	public List<ScalewayIamApplication> fetch(DataFetchContext context) {
		try {
			List<ScalewayClient> clients = ScalewayConnectorConfig.SCALEWAY_CLIENT.getAll( context );
			List<ScalewayIamApplication> result = new ArrayList<>();
			for ( ScalewayClient client : clients ) {
				for ( JsonNode node : client.listIamApplications() ) {
					result.add( ScalewayIamApplication.from( node ) );
				}
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Scaleway IAM applications", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( ScalewayIamApplication.class, ScalewayConventionContext.INSTANCE );
	}
}
