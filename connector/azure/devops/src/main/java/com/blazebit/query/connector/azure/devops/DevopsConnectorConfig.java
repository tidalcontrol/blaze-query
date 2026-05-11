/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.azure.devops;

import com.blazebit.query.connector.devops.invoker.ApiClient;
import com.blazebit.query.spi.DataFetcherConfig;

import java.util.Objects;

/**
 * The configuration properties for the Azure DevOps connector.
 *
 * @author Dimitar Prisadnikov
 * @since 1.0.8
 */
public final class DevopsConnectorConfig {

	/**
	 * Specifies the {@link Account} to use for querying data.
	 */
	public static final DataFetcherConfig<Account> ACCOUNT = DataFetcherConfig.forPropertyName(
			"azureDevopsAccount" );

	/**
	 * Specifies the WIQL query used to select work items. Defaults to
	 * {@code SELECT [System.Id] FROM WorkItems} when not set.
	 */
	public static final DataFetcherConfig<String> WIQL_QUERY = DataFetcherConfig.forPropertyName(
			"azureDevopsWiqlQuery" );

	private DevopsConnectorConfig() {
	}

	/**
	 * Bundles the two API clients together with the organization required by the
	 * Azure DevOps connector. {@code apiClient} should point to
	 * {@code https://app.vssps.visualstudio.com} (accounts / profile / Git APIs) and
	 * {@code witApiClient} should point to {@code https://dev.azure.com} (Work Item Tracking
	 * and Core APIs).
	 *
	 * @author Martijn Sprengers
	 * @since 1.0.8
	 */
	public static final class Account {

		private final ApiClient apiClient;
		private final ApiClient witApiClient;
		private final String organization;

		/**
		 * Creates a new Azure DevOps account configuration.
		 *
		 * @param apiClient the API client for the accounts/profile/Git endpoints
		 *   ({@code https://app.vssps.visualstudio.com})
		 * @param witApiClient the API client for the Work Item Tracking and Core endpoints
		 *   ({@code https://dev.azure.com})
		 * @param organization the Azure DevOps organization name
		 */
		public Account(ApiClient apiClient, ApiClient witApiClient, String organization) {
			this.apiClient = Objects.requireNonNull( apiClient, "apiClient must not be null" );
			this.witApiClient = Objects.requireNonNull( witApiClient, "witApiClient must not be null" );
			this.organization = Objects.requireNonNull( organization, "organization must not be null" );
		}

		/**
		 * Returns the API client for the accounts/profile/Git endpoints.
		 *
		 * @return the API client
		 */
		public ApiClient getApiClient() {
			return apiClient;
		}

		/**
		 * Returns the API client for the Work Item Tracking and Core endpoints.
		 *
		 * @return the WIT API client
		 */
		public ApiClient getWitApiClient() {
			return witApiClient;
		}

		/**
		 * Returns the Azure DevOps organization name.
		 *
		 * @return the organization name
		 */
		public String getOrganization() {
			return organization;
		}
	}
}
