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
 * Fetches all projects and their deployment-protection settings.
 *
 * <p>Uses {@code GET /v9/projects}. When the {@link VercelApiClient} is
 * configured with a {@code teamId} it is automatically applied as a query parameter.
 *
 * <p>Useful for auditing which projects have password protection or SSO protection
 * enabled, and for detecting projects that auto-expose system environment variables.
 *
 * @author Blazebit
 * @since 1.0.0
 */
public class ProjectDataFetcher implements DataFetcher<Project>, Serializable {

	public static final ProjectDataFetcher INSTANCE = new ProjectDataFetcher();

	private ProjectDataFetcher() {
	}

	@Override
	public List<Project> fetch(DataFetchContext context) {
		try {
			List<VercelApiClient> apiClients = VercelConnectorConfig.API_CLIENT.getAll( context );
			List<Project> list = new ArrayList<>();
			for ( VercelApiClient apiClient : apiClients ) {
				list.addAll( apiClient.fetchPagedList( "/v9/projects", "projects", Project.class ) );
			}
			return list;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Vercel project list", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.beansConvention( Project.class, VercelConventionContext.INSTANCE );
	}
}
