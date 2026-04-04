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
 * Fetches Scaleway Load Balancers for compliance auditing:
 * unencrypted frontends, TLS enforcement, and load balancer security posture.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public class ScalewayLoadBalancerDataFetcher implements DataFetcher<ScalewayLoadBalancer>, Serializable {

	public static final ScalewayLoadBalancerDataFetcher INSTANCE = new ScalewayLoadBalancerDataFetcher();

	private ScalewayLoadBalancerDataFetcher() {
	}

	@Override
	public List<ScalewayLoadBalancer> fetch(DataFetchContext context) {
		try {
			List<ScalewayClient> clients = ScalewayConnectorConfig.SCALEWAY_CLIENT.getAll( context );
			List<ScalewayLoadBalancer> result = new ArrayList<>();
			for ( ScalewayClient client : clients ) {
				for ( JsonNode node : client.listLoadBalancers() ) {
					result.add( ScalewayLoadBalancer.from( node ) );
				}
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Scaleway load balancers", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( ScalewayLoadBalancer.class, ScalewayConventionContext.INSTANCE );
	}
}
