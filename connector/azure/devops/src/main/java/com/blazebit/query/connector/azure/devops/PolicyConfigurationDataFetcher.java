/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.azure.devops;

import com.blazebit.query.connector.base.DataFormats;
import com.blazebit.query.connector.devops.api.PolicyConfigurationsApi;
import com.blazebit.query.connector.devops.invoker.ApiClient;
import com.blazebit.query.connector.devops.invoker.ApiException;
import com.blazebit.query.connector.devops.invoker.ApiResponse;
import com.blazebit.query.connector.devops.model.PolicyConfiguration;
import com.blazebit.query.connector.devops.model.PolicyConfigurationList;
import com.blazebit.query.connector.devops.model.TeamProjectReference;
import com.blazebit.query.spi.DataFetchContext;
import com.blazebit.query.spi.DataFetcher;
import com.blazebit.query.spi.DataFetcherException;
import com.blazebit.query.spi.DataFormat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fetches all {@link PolicyConfiguration} objects across every project accessible to the
 * configured Azure DevOps account. For each project, a single call to the project-scoped
 * {@code _apis/policy/configurations} endpoint returns all policy configurations — including
 * branch policies — without requiring per-repository filtering. Pages through results using
 * the {@code x-ms-continuationtoken} response header.
 *
 * @author Martijn Sprengers
 * @since 1.0.8
 */
public class PolicyConfigurationDataFetcher implements DataFetcher<PolicyConfiguration>, Serializable {

	public static final PolicyConfigurationDataFetcher INSTANCE = new PolicyConfigurationDataFetcher();

	private static final String CONTINUATION_TOKEN_HEADER = "x-ms-continuationtoken";

	private PolicyConfigurationDataFetcher() {
	}

	@Override
	public List<PolicyConfiguration> fetch(DataFetchContext context) {
		try {
			List<DevopsConnectorConfig.Account> accounts = DevopsConnectorConfig.ACCOUNT.getAll( context );
			List<PolicyConfiguration> result = new ArrayList<>();
			for ( DevopsConnectorConfig.Account account : accounts ) {
				ApiClient apiClient = account.getWitApiClient();
				String organization = account.getOrganization();
				PolicyConfigurationsApi api = new PolicyConfigurationsApi( apiClient );

				for ( TeamProjectReference project : context.getSession().getOrFetch( TeamProjectReference.class ) ) {
					String projectId = project.getId().toString();
					String continuationToken = null;
					do {
						ApiResponse<PolicyConfigurationList> response = api.policyConfigurationsListWithHttpInfo(
								organization, projectId, "7.1", null, null, null, continuationToken );
						PolicyConfigurationList page = response.getData();
						if ( page != null && page.getValue() != null ) {
							result.addAll( page.getValue() );
						}
						continuationToken = extractContinuationToken( response.getHeaders() );
					}
					while ( continuationToken != null );
				}
			}
			return result;
		}
		catch (ApiException e) {
			throw new DataFetcherException( "Could not fetch policy configuration list", e );
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
		return DataFormats.beansConvention( PolicyConfiguration.class, DevopsConventionContext.INSTANCE );
	}
}
