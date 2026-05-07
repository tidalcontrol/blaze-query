/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.azure.devops;

import com.blazebit.query.connector.base.DataFormats;
import com.blazebit.query.connector.devops.api.ProjectsApi;
import com.blazebit.query.connector.devops.invoker.ApiException;
import com.blazebit.query.connector.devops.invoker.ApiResponse;
import com.blazebit.query.connector.devops.model.TeamProjectReference;
import com.blazebit.query.connector.devops.model.TeamProjectReferenceList;
import com.blazebit.query.spi.DataFetchContext;
import com.blazebit.query.spi.DataFetcher;
import com.blazebit.query.spi.DataFetcherException;
import com.blazebit.query.spi.DataFormat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fetches all {@link TeamProjectReference} projects accessible to the configured account
 * within an Azure DevOps organization. Pages through results using the
 * {@code x-ms-continuationtoken} response header.
 *
 * @author Martijn Sprengers
 * @since 1.0.8
 */
public class ProjectDataFetcher implements DataFetcher<TeamProjectReference>, Serializable {

	public static final ProjectDataFetcher INSTANCE = new ProjectDataFetcher();

	private static final String CONTINUATION_TOKEN_HEADER = "x-ms-continuationtoken";

	private ProjectDataFetcher() {
	}

	@Override
	public List<TeamProjectReference> fetch(DataFetchContext context) {
		try {
			List<DevopsConnectorConfig.Account> accounts = DevopsConnectorConfig.ACCOUNT.getAll( context );
			List<TeamProjectReference> list = new ArrayList<>();
			for ( DevopsConnectorConfig.Account account : accounts ) {
				ProjectsApi api = new ProjectsApi( account.getWitApiClient() );
				String continuationToken = null;
				do {
					ApiResponse<TeamProjectReferenceList> response = api.projectsListWithHttpInfo(
							account.getOrganization(), "7.1", null, null, null, continuationToken, null );
					TeamProjectReferenceList page = response.getData();
					if ( page != null && page.getValue() != null ) {
						list.addAll( page.getValue() );
					}
					continuationToken = extractContinuationToken( response.getHeaders() );
				}
				while ( continuationToken != null );
			}
			return list;
		}
		catch (ApiException e) {
			throw new DataFetcherException( "Could not fetch project list", e );
		}
	}

	private String extractContinuationToken(Map<String, List<String>> headers) {
		for ( Map.Entry<String, List<String>> entry : headers.entrySet() ) {
			if ( CONTINUATION_TOKEN_HEADER.equalsIgnoreCase( entry.getKey() ) ) {
				List<String> values = entry.getValue();
				if ( values != null && !values.isEmpty() ) {
					return values.get( 0 );
				}
			}
		}
		return null;
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.beansConvention( TeamProjectReference.class, DevopsConventionContext.INSTANCE );
	}
}
