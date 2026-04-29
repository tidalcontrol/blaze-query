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
 * Fetches Scaleway Audit Trail events for security investigation and compliance reporting:
 * detecting forbidden access attempts, unusual API call patterns, and privilege escalation.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class ScalewayAuditEventDataFetcher implements DataFetcher<ScalewayAuditEvent>, Serializable {

	public static final ScalewayAuditEventDataFetcher INSTANCE = new ScalewayAuditEventDataFetcher();

	private ScalewayAuditEventDataFetcher() {
	}

	@Override
	public List<ScalewayAuditEvent> fetch(DataFetchContext context) {
		try {
			List<ScalewayClient> clients = ScalewayConnectorConfig.SCALEWAY_CLIENT.getAll( context );
			List<ScalewayAuditEvent> result = new ArrayList<>();
			for ( ScalewayClient client : clients ) {
				for ( JsonNode node : client.listAuditEvents() ) {
					result.add( ScalewayAuditEvent.from( node ) );
				}
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Scaleway audit events", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( ScalewayAuditEvent.class, ScalewayConventionContext.INSTANCE );
	}
}
