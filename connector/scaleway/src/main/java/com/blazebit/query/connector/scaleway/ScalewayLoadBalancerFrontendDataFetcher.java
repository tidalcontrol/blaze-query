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
 * Fetches Scaleway Load Balancer Frontends for compliance auditing:
 * TLS enforcement, unencrypted HTTP endpoints, and certificate management.
 * Depends on ScalewayLoadBalancer being fetched first.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class ScalewayLoadBalancerFrontendDataFetcher implements DataFetcher<ScalewayLoadBalancerFrontend>, Serializable {

	public static final ScalewayLoadBalancerFrontendDataFetcher INSTANCE = new ScalewayLoadBalancerFrontendDataFetcher();

	private ScalewayLoadBalancerFrontendDataFetcher() {
	}

	@Override
	public List<ScalewayLoadBalancerFrontend> fetch(DataFetchContext context) {
		try {
			List<ScalewayClient> clients = ScalewayConnectorConfig.SCALEWAY_CLIENT.getAll( context );
			List<ScalewayLoadBalancerFrontend> result = new ArrayList<>();
			for ( ScalewayClient client : clients ) {
				List<? extends ScalewayLoadBalancer> lbs = context.getSession().getOrFetch( ScalewayLoadBalancer.class );
				for ( ScalewayLoadBalancer lb : lbs ) {
					if ( lb.id() == null || lb.zone() == null ) {
						continue;
					}
					for ( JsonNode node : client.listLoadBalancerFrontends( lb.id(), lb.zone() ) ) {
						result.add( ScalewayLoadBalancerFrontend.from( node, lb.id(), lb.zone() ) );
					}
				}
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Scaleway load balancer frontends", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( ScalewayLoadBalancerFrontend.class, ScalewayConventionContext.INSTANCE );
	}
}
