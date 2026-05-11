/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.azure.devops;

import com.blazebit.query.connector.base.DataFormats;
import com.blazebit.query.connector.devops.api.RepositoriesApi;
import com.blazebit.query.connector.devops.invoker.ApiException;
import com.blazebit.query.connector.devops.model.GitRepository;
import com.blazebit.query.connector.devops.model.GitRepositoryList;
import com.blazebit.query.connector.devops.model.TeamProjectReference;
import com.blazebit.query.spi.DataFetchContext;
import com.blazebit.query.spi.DataFetcher;
import com.blazebit.query.spi.DataFetcherException;
import com.blazebit.query.spi.DataFormat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches {@link GitRepository} objects across every project accessible to the configured
 * Azure DevOps account.
 *
 * @author Martijn Sprengers
 * @since 1.0.8
 */
public class RepositoryDataFetcher implements DataFetcher<GitRepository>, Serializable {

	public static final RepositoryDataFetcher INSTANCE = new RepositoryDataFetcher();

	private RepositoryDataFetcher() {
	}

	@Override
	public List<GitRepository> fetch(DataFetchContext context) {
		try {
			List<DevopsConnectorConfig.Account> accounts = DevopsConnectorConfig.ACCOUNT.getAll( context );
			List<GitRepository> list = new ArrayList<>();
			for ( DevopsConnectorConfig.Account account : accounts ) {
				RepositoriesApi repositoriesApi = new RepositoriesApi( account.getWitApiClient() );
				for ( TeamProjectReference project : context.getSession().getOrFetch( TeamProjectReference.class ) ) {
					GitRepositoryList repositories = repositoriesApi.repositoriesList(
							account.getOrganization(), project.getId().toString(), "7.1", null, null, null );
					if ( repositories != null && repositories.getValue() != null ) {
						list.addAll( repositories.getValue() );
					}
				}
			}
			return list;
		}
		catch (ApiException e) {
			throw new DataFetcherException( "Could not fetch repository list", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.beansConvention( GitRepository.class, DevopsConventionContext.INSTANCE );
	}
}
