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
 * Fetches all third-party integration configurations for the team.
 *
 * <p>Uses {@code GET /v1/integrations/configurations}. The response is a bare
 * JSON array (no wrapper object and no pagination).
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class IntegrationConfigurationDataFetcher implements DataFetcher<IntegrationConfiguration>, Serializable {

	public static final IntegrationConfigurationDataFetcher INSTANCE = new IntegrationConfigurationDataFetcher();

	private IntegrationConfigurationDataFetcher() {
	}

	@Override
	public List<IntegrationConfiguration> fetch(DataFetchContext context) {
		try {
			List<VercelApiClient> apiClients = VercelConnectorConfig.API_CLIENT.getAll( context );
			List<IntegrationConfiguration> list = new ArrayList<>();
			for ( VercelApiClient apiClient : apiClients ) {
				list.addAll( apiClient.fetchPagedList(
						"/v1/integrations/configurations", null, IntegrationConfiguration.class ) );
			}
			return list;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Vercel integration configuration list", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.beansConvention( IntegrationConfiguration.class, VercelConventionContext.INSTANCE );
	}
}
