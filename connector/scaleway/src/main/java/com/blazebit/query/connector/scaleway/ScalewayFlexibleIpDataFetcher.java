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
 * Fetches Scaleway Flexible IP addresses for compliance auditing:
 * unattached/orphaned IPs, IP lifecycle management, and attack surface reduction.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class ScalewayFlexibleIpDataFetcher implements DataFetcher<ScalewayFlexibleIp>, Serializable {

	public static final ScalewayFlexibleIpDataFetcher INSTANCE = new ScalewayFlexibleIpDataFetcher();

	private ScalewayFlexibleIpDataFetcher() {
	}

	@Override
	public List<ScalewayFlexibleIp> fetch(DataFetchContext context) {
		try {
			List<ScalewayClient> clients = ScalewayConnectorConfig.SCALEWAY_CLIENT.getAll( context );
			List<ScalewayFlexibleIp> result = new ArrayList<>();
			for ( ScalewayClient client : clients ) {
				for ( JsonNode node : client.listFlexibleIps() ) {
					result.add( ScalewayFlexibleIp.from( node ) );
				}
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Scaleway flexible IPs", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( ScalewayFlexibleIp.class, ScalewayConventionContext.INSTANCE );
	}
}
