/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.hubspot;

import com.blazebit.query.connector.base.DataFormats;
import com.blazebit.query.spi.DataFetchContext;
import com.blazebit.query.spi.DataFetcher;
import com.blazebit.query.spi.DataFetcherException;
import com.blazebit.query.spi.DataFormat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches all {@link HubspotSubscriptionDefinition} records from
 * {@code GET /communication-preferences/v3/definitions}.
 *
 * <p>Returns the portal's catalogue of email consent / subscription types.
 * Use this to verify that GDPR-compliant consent categories are properly
 * configured before auditing per-contact consent status.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class HubspotSubscriptionDefinitionDataFetcher implements DataFetcher<HubspotSubscriptionDefinition>, Serializable {

	public static final HubspotSubscriptionDefinitionDataFetcher INSTANCE =
			new HubspotSubscriptionDefinitionDataFetcher();

	private HubspotSubscriptionDefinitionDataFetcher() {
	}

	@Override
	public List<HubspotSubscriptionDefinition> fetch(DataFetchContext context) {
		try {
			List<HubspotClient> clients = HubspotConnectorConfig.HUBSPOT_CLIENT.getAll( context );
			List<HubspotSubscriptionDefinition> result = new ArrayList<>();
			for ( HubspotClient client : clients ) {
				result.addAll( client.listSubscriptionDefinitions() );
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch HubSpot subscription definitions", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention(
				HubspotSubscriptionDefinition.class, HubspotConventionContext.INSTANCE );
	}
}
