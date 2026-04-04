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
 * Fetches all TLS certificates managed by Vercel.
 *
 * <p>Uses {@code GET /v7/certs}. When the {@link VercelApiClient} is
 * configured with a {@code teamId} it is automatically applied as a query parameter.
 *
 * @author Blazebit
 * @since 1.0.0
 */
public class CertificateDataFetcher implements DataFetcher<Certificate>, Serializable {

	public static final CertificateDataFetcher INSTANCE = new CertificateDataFetcher();

	private CertificateDataFetcher() {
	}

	@Override
	public List<Certificate> fetch(DataFetchContext context) {
		try {
			List<VercelApiClient> apiClients = VercelConnectorConfig.API_CLIENT.getAll( context );
			List<Certificate> list = new ArrayList<>();
			for ( VercelApiClient apiClient : apiClients ) {
				list.addAll( apiClient.fetchPagedList( "/v7/certs", "certs", Certificate.class ) );
			}
			return list;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Vercel certificate list", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.beansConvention( Certificate.class, VercelConventionContext.INSTANCE );
	}
}
