/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.vercel;

import com.blazebit.query.spi.ConfigurationProvider;
import com.blazebit.query.spi.DataFetcher;
import com.blazebit.query.spi.QuerySchemaProvider;

import java.util.Set;

/**
 * The schema provider for the Vercel connector.
 *
 * @author Blazebit
 * @since 1.0.0
 */
public final class VercelSchemaProvider implements QuerySchemaProvider {

	@Override
	public Set<? extends DataFetcher<?>> resolveSchemaObjects(ConfigurationProvider configurationProvider) {
		return Set.of(
				AuthTokenDataFetcher.INSTANCE,
				TeamDataFetcher.INSTANCE,
				TeamMemberDataFetcher.INSTANCE,
				AccessGroupDataFetcher.INSTANCE,
				AccessGroupMemberDataFetcher.INSTANCE,
				WebhookDataFetcher.INSTANCE,
				ProjectDataFetcher.INSTANCE,
				ProjectMemberDataFetcher.INSTANCE,
				EnvironmentVariableDataFetcher.INSTANCE,
				LogDrainDataFetcher.INSTANCE,
				FirewallConfigDataFetcher.INSTANCE,
				IntegrationConfigurationDataFetcher.INSTANCE,
				DeploymentDataFetcher.INSTANCE,
				DomainDataFetcher.INSTANCE,
				CertificateDataFetcher.INSTANCE
		);
	}
}
