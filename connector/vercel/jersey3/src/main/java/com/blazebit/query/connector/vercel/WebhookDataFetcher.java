/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.vercel;

import com.blazebit.query.connector.base.DataFormats;
import com.blazebit.query.spi.DataFetchContext;
import com.blazebit.query.spi.DataFetcher;
import com.blazebit.query.spi.DataFetcherException;
import com.blazebit.query.spi.DataFormat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches all webhooks configured for the team.
 *
 * <p>Uses {@code GET /v1/webhooks}. The response is a bare JSON array (no wrapper
 * object and no pagination).
 *
 * <p>Useful for verifying that security-relevant webhook events
 * (e.g. {@code firewall.attack}, {@code project.env-variable.created}) are
 * being sent to an appropriate endpoint, and that no unexpected webhooks exist.
 *
 * @author Blazebit
 * @since 1.0.0
 */
public class WebhookDataFetcher implements DataFetcher<Webhook>, Serializable {

	public static final WebhookDataFetcher INSTANCE = new WebhookDataFetcher();

	private WebhookDataFetcher() {
	}

	@Override
	public List<Webhook> fetch(DataFetchContext context) {
		try {
			List<VercelApiClient> apiClients = VercelConnectorConfig.API_CLIENT.getAll( context );
			List<Webhook> list = new ArrayList<>();
			for ( VercelApiClient apiClient : apiClients ) {
				// /v1/webhooks returns a bare JSON array, so itemsKey is null
				list.addAll( apiClient.fetchPagedList( "/v1/webhooks", null, Webhook.class ) );
			}
			return list;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Vercel webhook list", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.beansConvention( Webhook.class, VercelConventionContext.INSTANCE );
	}
}
