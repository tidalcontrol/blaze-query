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
 * Fetches all access groups for the configured team.
 *
 * <p>Uses {@code GET /v1/access-groups}. When the {@link VercelApiClient} is
 * configured with a {@code teamId} it is automatically applied as a query parameter.
 *
 * <p>Useful for auditing role-based project access and identifying groups that
 * are (or are not) managed by directory sync.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class AccessGroupDataFetcher implements DataFetcher<AccessGroup>, Serializable {

	public static final AccessGroupDataFetcher INSTANCE = new AccessGroupDataFetcher();

	private AccessGroupDataFetcher() {
	}

	@Override
	public List<AccessGroup> fetch(DataFetchContext context) {
		try {
			List<VercelApiClient> apiClients = VercelConnectorConfig.API_CLIENT.getAll( context );
			List<AccessGroup> list = new ArrayList<>();
			for ( VercelApiClient apiClient : apiClients ) {
				list.addAll( apiClient.fetchPagedList( "/v1/access-groups", "accessGroups", AccessGroup.class ) );
			}
			return list;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Vercel access group list", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.beansConvention( AccessGroup.class, VercelConventionContext.INSTANCE );
	}
}
