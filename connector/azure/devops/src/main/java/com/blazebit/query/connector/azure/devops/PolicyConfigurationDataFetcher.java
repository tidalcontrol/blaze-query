/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.azure.devops;

import com.blazebit.query.connector.base.DataFormats;
import com.blazebit.query.connector.devops.api.PolicyConfigurationsApi;
import com.blazebit.query.connector.devops.api.RepositoriesApi;
import com.blazebit.query.connector.devops.invoker.ApiClient;
import com.blazebit.query.connector.devops.invoker.ApiException;
import com.blazebit.query.connector.devops.invoker.ApiResponse;
import com.blazebit.query.connector.devops.model.GitRepository;
import com.blazebit.query.connector.devops.model.GitRepositoryList;
import com.blazebit.query.connector.devops.model.PolicyConfiguration;
import com.blazebit.query.connector.devops.model.PolicyConfigurationList;
import com.blazebit.query.connector.devops.model.TeamProjectReference;
import com.blazebit.query.spi.DataFetchContext;
import com.blazebit.query.spi.DataFetcher;
import com.blazebit.query.spi.DataFetcherException;
import com.blazebit.query.spi.DataFormat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fetches all {@link PolicyConfiguration} objects across every project accessible to the
 * configured Azure DevOps account. For each project, fetches project-wide policies and then
 * iterates over every repository to capture repository-scoped policies. Pages through results
 * using the {@code x-ms-continuationtoken} response header.
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
			// Use a map to deduplicate: project-wide policies appear for every repo they match
			Map<Integer, PolicyConfiguration> deduplicated = new LinkedHashMap<>();
			for ( DevopsConnectorConfig.Account account : accounts ) {
				ApiClient apiClient = account.getWitApiClient();
				String organization = account.getOrganization();

				for ( TeamProjectReference project : context.getSession().getOrFetch( TeamProjectReference.class ) ) {
					String projectId = project.getId().toString();
					fetchForProject( apiClient, organization, projectId, deduplicated );

					// The project-level fetch returns only policies not scoped to a specific repository.
					// Per-repository fetches are required to capture repository-scoped policies (e.g. branch
					// policies). Deduplication via the map ensures project-wide policies are not counted twice.
					RepositoriesApi repositoriesApi = new RepositoriesApi( apiClient );
					GitRepositoryList repositories = repositoriesApi.repositoriesList(
							organization, projectId, "7.1", null, null, null );
					if ( repositories != null && repositories.getValue() != null ) {
						for ( GitRepository repository : repositories.getValue() ) {
							fetchForRepository( apiClient, organization, projectId, repository, deduplicated );
						}
					}
				}
			}
			return new ArrayList<>( deduplicated.values() );
		}
		catch (ApiException e) {
			throw new DataFetcherException( "Could not fetch policy configuration list", e );
		}
	}

	private void fetchForProject(ApiClient apiClient, String organization, String project,
			Map<Integer, PolicyConfiguration> target) throws ApiException {
		PolicyConfigurationsApi api = new PolicyConfigurationsApi( apiClient );
		String continuationToken = null;
		do {
			ApiResponse<PolicyConfigurationList> response = api.policyConfigurationsGetWithHttpInfo(
					organization, project, "7.1", null, null, null, null, continuationToken );
			collectPolicies( response.getData(), target );
			continuationToken = extractContinuationToken( response.getHeaders() );
		}
		while ( continuationToken != null );
	}

	private void fetchForRepository(ApiClient apiClient, String organization, String project,
			GitRepository repository, Map<Integer, PolicyConfiguration> target) throws ApiException {
		PolicyConfigurationsApi api = new PolicyConfigurationsApi( apiClient );
		String continuationToken = null;
		do {
			ApiResponse<PolicyConfigurationList> response = api.policyConfigurationsGetWithHttpInfo(
					organization, project, "7.1", repository.getId(), null, null, null, continuationToken );
			collectPolicies( response.getData(), target );
			continuationToken = extractContinuationToken( response.getHeaders() );
		}
		while ( continuationToken != null );
	}

	private void collectPolicies(PolicyConfigurationList page, Map<Integer, PolicyConfiguration> target) {
		if ( page == null || page.getValue() == null ) {
			return;
		}
		for ( PolicyConfiguration policy : page.getValue() ) {
			target.putIfAbsent( policy.getId(), policy );
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
