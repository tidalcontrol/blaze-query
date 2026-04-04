/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.notion;

import com.blazebit.query.spi.DataFetcherConfig;

/**
 * The configuration properties for the Notion connector.
 *
 * @author Blazebit
 * @since 1.0.0
 */
public final class NotionConnectorConfig {

	/**
	 * Specifies the {@link NotionClient} to use for querying Notion data.
	 * The client must be pre-configured with a valid Notion integration token.
	 */
	public static final DataFetcherConfig<NotionClient> NOTION_CLIENT =
			DataFetcherConfig.forPropertyName( "notionClient" );

	private NotionConnectorConfig() {
	}
}
