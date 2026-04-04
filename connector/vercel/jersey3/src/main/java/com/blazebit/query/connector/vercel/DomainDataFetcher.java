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
 * Fetches all domains registered or configured in Vercel.
 *
 * <p>Uses {@code GET /v5/domains}. When the {@link VercelApiClient} is
 * configured with a {@code teamId} it is automatically applied as a query parameter.
 *
 * @author Blazebit
 * @since 1.0.0
 */
public class DomainDataFetcher implements DataFetcher<Domain>, Serializable {

	public static final DomainDataFetcher INSTANCE = new DomainDataFetcher();

	private DomainDataFetcher() {
	}

	@Override
	public List<Domain> fetch(DataFetchContext context) {
		try {
			List<VercelApiClient> apiClients = VercelConnectorConfig.API_CLIENT.getAll( context );
			List<Domain> list = new ArrayList<>();
			for ( VercelApiClient apiClient : apiClients ) {
				list.addAll( apiClient.fetchPagedList( "/v5/domains", "domains", Domain.class ) );
			}
			return list;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Vercel domain list", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.beansConvention( Domain.class, VercelConventionContext.INSTANCE );
	}
}
