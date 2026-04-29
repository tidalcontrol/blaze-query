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
 * Fetches Scaleway VPCs for network segmentation auditing:
 * detecting default VPCs with routing enabled, which allows
 * unrestricted lateral movement between private networks.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class ScalewayVpcDataFetcher implements DataFetcher<ScalewayVpc>, Serializable {

	public static final ScalewayVpcDataFetcher INSTANCE = new ScalewayVpcDataFetcher();

	private ScalewayVpcDataFetcher() {
	}

	@Override
	public List<ScalewayVpc> fetch(DataFetchContext context) {
		try {
			List<ScalewayClient> clients = ScalewayConnectorConfig.SCALEWAY_CLIENT.getAll( context );
			List<ScalewayVpc> result = new ArrayList<>();
			for ( ScalewayClient client : clients ) {
				for ( JsonNode node : client.listVpcs() ) {
					result.add( ScalewayVpc.from( node ) );
				}
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Scaleway VPCs", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( ScalewayVpc.class, ScalewayConventionContext.INSTANCE );
	}
}
