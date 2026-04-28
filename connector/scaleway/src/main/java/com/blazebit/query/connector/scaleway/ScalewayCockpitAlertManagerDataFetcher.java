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
 * Fetches Scaleway Cockpit Alert Manager configuration per region for compliance auditing:
 * missing alert contacts, disabled managed alerts, and observability coverage gaps.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class ScalewayCockpitAlertManagerDataFetcher implements DataFetcher<ScalewayCockpitAlertManager>, Serializable {

	public static final ScalewayCockpitAlertManagerDataFetcher INSTANCE = new ScalewayCockpitAlertManagerDataFetcher();

	private ScalewayCockpitAlertManagerDataFetcher() {
	}

	@Override
	public List<ScalewayCockpitAlertManager> fetch(DataFetchContext context) {
		try {
			List<ScalewayClient> clients = ScalewayConnectorConfig.SCALEWAY_CLIENT.getAll( context );
			List<ScalewayCockpitAlertManager> result = new ArrayList<>();
			for ( ScalewayClient client : clients ) {
				List<String> regions = client.regions();
				for ( JsonNode project : client.listProjects() ) {
					String projectId = project.path( "id" ).asText( null );
					if ( projectId == null || projectId.isEmpty() ) {
						continue;
					}
					for ( String region : regions ) {
						JsonNode alertManagerNode = client.getCockpitAlertManager( region, projectId );
						if ( alertManagerNode == null || alertManagerNode.isMissingNode() ) {
							result.add( new ScalewayCockpitAlertManager( region, projectId, false, 0 ) );
							continue;
						}
						int contactPointCount = client.getCockpitContactPointCount( region, projectId );
						result.add( ScalewayCockpitAlertManager.from( alertManagerNode, region, projectId, contactPointCount ) );
					}
				}
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Scaleway Cockpit alert manager info", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( ScalewayCockpitAlertManager.class, ScalewayConventionContext.INSTANCE );
	}
}
