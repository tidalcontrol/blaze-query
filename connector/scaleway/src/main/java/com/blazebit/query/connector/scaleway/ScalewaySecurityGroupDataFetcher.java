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
 * Fetches Scaleway security groups for network security auditing:
 * permissive default policies, stateful configuration, and SMTP blocking.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class ScalewaySecurityGroupDataFetcher implements DataFetcher<ScalewaySecurityGroup>, Serializable {

	public static final ScalewaySecurityGroupDataFetcher INSTANCE = new ScalewaySecurityGroupDataFetcher();

	private ScalewaySecurityGroupDataFetcher() {
	}

	@Override
	public List<ScalewaySecurityGroup> fetch(DataFetchContext context) {
		try {
			List<ScalewayClient> clients = ScalewayConnectorConfig.SCALEWAY_CLIENT.getAll( context );
			List<ScalewaySecurityGroup> result = new ArrayList<>();
			for ( ScalewayClient client : clients ) {
				for ( JsonNode node : client.listSecurityGroups() ) {
					result.add( ScalewaySecurityGroup.from( node ) );
				}
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Scaleway security groups", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( ScalewaySecurityGroup.class, ScalewayConventionContext.INSTANCE );
	}
}
