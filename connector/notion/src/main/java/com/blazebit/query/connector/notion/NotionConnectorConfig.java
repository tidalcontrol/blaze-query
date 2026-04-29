/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.notion;

import com.blazebit.query.spi.DataFetcherConfig;

/**
 * The configuration properties for the Notion connector.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public final class NotionConnectorConfig {

	/**
	 * Specifies the {@link NotionClient} to use for querying Notion data.
	 * The client must be pre-configured with a valid Notion integration token.
	 */
	public static final DataFetcherConfig<NotionClient> NOTION_CLIENT =
			DataFetcherConfig.forPropertyName( "notionClient" );

	/**
	 * Maximum depth for recursive block traversal used by {@link NotionBlockDataFetcher}.
	 *
	 * <p>Depth {@code 1} (the default) fetches only the direct children of each page.
	 * Depth {@code 2} additionally fetches the children of every block that has children,
	 * and so on. Higher values can be very expensive in large workspaces — each level
	 * issues one API call per block with {@code has_children = true}.
	 *
	 * <p>Set this to a value greater than {@code 1} only when complete DLP coverage of
	 * nested content (toggle blocks, column layouts, etc.) is required.
	 */
	public static final DataFetcherConfig<Integer> BLOCK_MAX_DEPTH =
			DataFetcherConfig.forPropertyName( "notionBlockMaxDepth" );

	/**
	 * Controls whether {@link NotionDatabaseRowDataFetcher} is active.
	 *
	 * <p>Defaults to {@code false} because querying rows issues one API call per database
	 * and can return a very large number of records. Set to {@code true} explicitly when
	 * row-level compliance scanning is required.
	 */
	public static final DataFetcherConfig<Boolean> DATABASE_ROWS_ENABLED =
			DataFetcherConfig.forPropertyName( "notionDatabaseRowsEnabled" );

	private NotionConnectorConfig() {
	}
}
