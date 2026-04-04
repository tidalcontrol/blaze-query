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
 * Fetches all log drains configured for the team.
 *
 * <p>Uses {@code GET /v1/log-drains}. The response is a bare JSON array.
 *
 * @author Blazebit
 * @since 1.0.0
 */
public class LogDrainDataFetcher implements DataFetcher<LogDrain>, Serializable {

	public static final LogDrainDataFetcher INSTANCE = new LogDrainDataFetcher();

	private LogDrainDataFetcher() {
	}

	@Override
	public List<LogDrain> fetch(DataFetchContext context) {
		try {
			List<VercelApiClient> apiClients = VercelConnectorConfig.API_CLIENT.getAll( context );
			List<LogDrain> list = new ArrayList<>();
			for ( VercelApiClient apiClient : apiClients ) {
				list.addAll( apiClient.fetchPagedList( "/v1/log-drains", null, LogDrain.class ) );
			}
			return list;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Vercel log drain list", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.beansConvention( LogDrain.class, VercelConventionContext.INSTANCE );
	}
}
