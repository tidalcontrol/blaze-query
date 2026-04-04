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
 * Fetches all authentication tokens for the authenticated user.
 *
 * <p>Uses {@code GET /v6/user/tokens}. Tokens are user-level resources; the
 * {@code teamId} on the configured {@link VercelApiClient} is not used here.
 *
 * <p>Useful for auditing which tokens exist, when they were last used, and
 * whether any have been flagged as leaked or are close to expiry.
 *
 * @author Blazebit
 * @since 1.0.0
 */
public class AuthTokenDataFetcher implements DataFetcher<AuthToken>, Serializable {

	public static final AuthTokenDataFetcher INSTANCE = new AuthTokenDataFetcher();

	private AuthTokenDataFetcher() {
	}

	@Override
	public List<AuthToken> fetch(DataFetchContext context) {
		try {
			List<VercelApiClient> apiClients = VercelConnectorConfig.API_CLIENT.getAll( context );
			List<AuthToken> list = new ArrayList<>();
			for ( VercelApiClient apiClient : apiClients ) {
				list.addAll( apiClient.fetchPagedList( "/v6/user/tokens", "tokens", AuthToken.class ) );
			}
			return list;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Vercel auth token list", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.beansConvention( AuthToken.class, VercelConventionContext.INSTANCE );
	}
}
