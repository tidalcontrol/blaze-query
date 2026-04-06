/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.blazebit.query.spi.DataFetcherConfig;

/**
 * Configuration properties for the Scaleway connector.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public final class ScalewayConnectorConfig {

	/**
	 * Specifies the {@link ScalewayClient} to use for querying Scaleway APIs.
	 * <p>
	 * Configure this with a {@code ScalewayClient} instance constructed using your
	 * organization's API secret key, organization ID, and the list of zones to query.
	 */
	public static final DataFetcherConfig<ScalewayClient> SCALEWAY_CLIENT =
			DataFetcherConfig.forPropertyName( "scaewayClient" );

	private ScalewayConnectorConfig() {
	}
}
