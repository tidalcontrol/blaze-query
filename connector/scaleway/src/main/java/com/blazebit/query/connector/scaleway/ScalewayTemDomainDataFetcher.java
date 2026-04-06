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
 * Fetches Scaleway Transactional Email domains for compliance auditing:
 * missing SPF/DKIM/MX configuration, domain health, and email delivery hygiene.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class ScalewayTemDomainDataFetcher implements DataFetcher<ScalewayTemDomain>, Serializable {

	public static final ScalewayTemDomainDataFetcher INSTANCE = new ScalewayTemDomainDataFetcher();

	private ScalewayTemDomainDataFetcher() {
	}

	@Override
	public List<ScalewayTemDomain> fetch(DataFetchContext context) {
		try {
			List<ScalewayClient> clients = ScalewayConnectorConfig.SCALEWAY_CLIENT.getAll( context );
			List<ScalewayTemDomain> result = new ArrayList<>();
			for ( ScalewayClient client : clients ) {
				for ( JsonNode node : client.listTemDomains() ) {
					result.add( ScalewayTemDomain.from( node ) );
				}
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Scaleway TEM domains", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( ScalewayTemDomain.class, ScalewayConventionContext.INSTANCE );
	}
}
