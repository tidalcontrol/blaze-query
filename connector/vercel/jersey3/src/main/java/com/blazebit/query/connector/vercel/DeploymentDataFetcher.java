/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.vercel;

import com.blazebit.query.connector.base.DataFormats;
import com.blazebit.query.spi.DataFetchContext;
import com.blazebit.query.spi.DataFetcher;
import com.blazebit.query.spi.DataFetcherException;
import com.blazebit.query.spi.DataFormat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches recent deployments across all projects.
 *
 * <p>Uses {@code GET /v6/deployments}. When the {@link VercelApiClient} is
 * configured with a {@code teamId} it is automatically applied as a query parameter.
 * Results are limited to the most recent 100 deployments per client.
 *
 * @author Blazebit
 * @since 1.0.0
 */
public class DeploymentDataFetcher implements DataFetcher<Deployment>, Serializable {

	public static final DeploymentDataFetcher INSTANCE = new DeploymentDataFetcher();

	private DeploymentDataFetcher() {
	}

	@Override
	public List<Deployment> fetch(DataFetchContext context) {
		try {
			List<VercelApiClient> apiClients = VercelConnectorConfig.API_CLIENT.getAll( context );
			List<Deployment> list = new ArrayList<>();
			for ( VercelApiClient apiClient : apiClients ) {
				list.addAll( apiClient.fetchPagedList( "/v6/deployments", "deployments", Deployment.class ) );
			}
			return list;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Vercel deployment list", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.beansConvention( Deployment.class, VercelConventionContext.INSTANCE );
	}
}
