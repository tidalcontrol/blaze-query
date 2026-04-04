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
 * Fetches all environment variables for every project.
 *
 * <p>Depends on {@link ProjectDataFetcher} — projects are retrieved from the
 * session cache and then {@code GET /v9/projects/{id}/env} is called for each.
 *
 * <p>Note: values of {@code sensitive} and {@code encrypted} variables are
 * redacted by the API; only the key, type, and target are returned for those.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class EnvironmentVariableDataFetcher implements DataFetcher<EnvironmentVariable>, Serializable {

	public static final EnvironmentVariableDataFetcher INSTANCE = new EnvironmentVariableDataFetcher();

	private EnvironmentVariableDataFetcher() {
	}

	@Override
	public List<EnvironmentVariable> fetch(DataFetchContext context) {
		try {
			List<VercelApiClient> apiClients = VercelConnectorConfig.API_CLIENT.getAll( context );
			List<? extends Project> projects = context.getSession().getOrFetch( Project.class );
			List<EnvironmentVariable> list = new ArrayList<>();
			for ( VercelApiClient apiClient : apiClients ) {
				for ( Project project : projects ) {
					List<EnvironmentVariable> vars = apiClient.fetchPagedList(
							"/v9/projects/" + project.getId() + "/env",
							"envs",
							EnvironmentVariable.class
					);
					for ( EnvironmentVariable var : vars ) {
						var.setProjectId( project.getId() );
					}
					list.addAll( vars );
				}
			}
			return list;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Vercel environment variable list", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.beansConvention( EnvironmentVariable.class, VercelConventionContext.INSTANCE );
	}
}
