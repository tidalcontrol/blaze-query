/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.blazebit.query.spi.ConfigurationProvider;
import com.blazebit.query.spi.DataFetcher;
import com.blazebit.query.spi.QuerySchemaProvider;

import java.util.Set;

/**
 * Schema provider for the Scaleway connector.
 * Registers all available DataFetcher instances covering IAM and Instance APIs.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public final class ScalewaySchemaProvider implements QuerySchemaProvider {

	@Override
	public Set<? extends DataFetcher<?>> resolveSchemaObjects(ConfigurationProvider configurationProvider) {
		return Set.of(
				// IAM — identity, access, and credential management
				ScalewayIamUserDataFetcher.INSTANCE,
				ScalewayIamGroupDataFetcher.INSTANCE,
				ScalewayIamApplicationDataFetcher.INSTANCE,
				ScalewayIamApiKeyDataFetcher.INSTANCE,
				ScalewayIamPolicyDataFetcher.INSTANCE,
				ScalewayIamSshKeyDataFetcher.INSTANCE,
				// Instance — compute and network security
				ScalewayInstanceDataFetcher.INSTANCE,
				ScalewaySecurityGroupDataFetcher.INSTANCE,
				ScalewaySecurityGroupRuleDataFetcher.INSTANCE
		);
	}
}
