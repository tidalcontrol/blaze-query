/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.blazebit.query.connector.base.DataFormats;
import com.blazebit.query.spi.DataFetchContext;
import com.blazebit.query.spi.DataFetcher;
import com.blazebit.query.spi.DataFetcherException;
import com.blazebit.query.spi.DataFormat;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches Scaleway IAM policies for privilege escalation detection and
 * least-privilege compliance auditing.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class ScalewayIamPolicyDataFetcher implements DataFetcher<ScalewayIamPolicy>, Serializable {

	public static final ScalewayIamPolicyDataFetcher INSTANCE = new ScalewayIamPolicyDataFetcher();

	private ScalewayIamPolicyDataFetcher() {
	}

	@Override
	public List<ScalewayIamPolicy> fetch(DataFetchContext context) {
		try {
			List<ScalewayClient> clients = ScalewayConnectorConfig.SCALEWAY_CLIENT.getAll( context );
			List<ScalewayIamPolicy> result = new ArrayList<>();
			for ( ScalewayClient client : clients ) {
				for ( JsonNode node : client.listIamPolicies() ) {
					result.add( ScalewayIamPolicy.from( node ) );
				}
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Scaleway IAM policies", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( ScalewayIamPolicy.class, ScalewayConventionContext.INSTANCE );
	}
}
