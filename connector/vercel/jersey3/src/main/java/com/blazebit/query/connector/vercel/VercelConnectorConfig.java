/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.vercel;

import com.blazebit.query.spi.DataFetcherConfig;

/**
 * Configuration properties for the Vercel connector.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public final class VercelConnectorConfig {

	/**
	 * Specifies the {@link VercelApiClient} to use for querying data.
	 *
	 * <p>Configure on a {@code QuerySession} via:
	 * <pre>{@code
	 * session.setProperty(VercelConnectorConfig.API_CLIENT.getPropertyName(),
	 *         new VercelApiClient("your-token", "team_xxxxxxxx"));
	 * }</pre>
	 */
	public static final DataFetcherConfig<VercelApiClient> API_CLIENT =
			DataFetcherConfig.forPropertyName( "vercelApiClient" );

	private VercelConnectorConfig() {
	}
}
