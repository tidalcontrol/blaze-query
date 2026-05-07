/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.azure.defender;

import com.blazebit.query.spi.DataFetcherConfig;

import java.util.Set;

/**
 * Configuration properties for the Microsoft Defender for Endpoint
 * {@link com.blazebit.query.spi.DataFetcher} instances.
 *
 * @author Martijn Sprengers
 * @since 1.0.8
 */
public final class DefenderConnectorConfig {

	/**
	 * Specifies the {@link DefenderClientAccessor} to use for querying data.
	 */
	public static final DataFetcherConfig<DefenderClientAccessor> DEFENDER_CLIENT = DataFetcherConfig.forPropertyName(
			"defenderClient" );

	/**
	 * Optional set of severities to include when fetching Defender vulnerabilities-by-machine. When unset
	 * (or empty), no severity filter is applied and all severities are returned.
	 * Valid values match the Defender API: {@code Low}, {@code Medium}, {@code High}, {@code Critical}.
	 */
	public static final DataFetcherConfig<Set<String>> VULNERABILITY_SEVERITIES =
			DataFetcherConfig.forPropertyName( "defenderVulnerabilitySeverities" );

	private DefenderConnectorConfig() {
	}
}
