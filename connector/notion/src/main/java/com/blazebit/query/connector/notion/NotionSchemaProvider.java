/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.notion;

import com.blazebit.query.spi.ConfigurationProvider;
import com.blazebit.query.spi.DataFetcher;
import com.blazebit.query.spi.QuerySchemaProvider;

import java.util.Set;

/**
 * The schema provider for the Notion connector.
 *
 * <p>Registers all security- and compliance-relevant data fetchers for the Notion
 * workspace API. Configure the connector by supplying a {@link NotionClient} via
 * {@link NotionConnectorConfig#NOTION_CLIENT} on the query context.
 *
 * <p>Registered schema objects:
 * <ul>
 *   <li>{@link NotionWorkspace} — workspace identity and integration metadata</li>
 *   <li>{@link NotionUser} — all workspace users (persons and bots)</li>
 *   <li>{@link NotionPage} — all pages accessible to the integration</li>
 *   <li>{@link NotionDatabase} — all databases accessible to the integration</li>
 *   <li>{@link NotionBlock} — block content of all accessible pages (configurable depth)</li>
 *   <li>{@link NotionComment} — all comments on accessible pages (DLP + audit trail)</li>
 *   <li>{@link NotionDatabaseRow} — rows from all accessible databases (opt-in via
 *       {@link NotionConnectorConfig#DATABASE_ROWS_ENABLED})</li>
 * </ul>
 *
 * @author Blazebit
 * @since 1.0.0
 */
public final class NotionSchemaProvider implements QuerySchemaProvider {

	@Override
	public Set<? extends DataFetcher<?>> resolveSchemaObjects(ConfigurationProvider configurationProvider) {
		return Set.of(
				NotionWorkspaceDataFetcher.INSTANCE,
				NotionUserDataFetcher.INSTANCE,
				NotionPageDataFetcher.INSTANCE,
				NotionDatabaseDataFetcher.INSTANCE,
				NotionBlockDataFetcher.INSTANCE,
				NotionCommentDataFetcher.INSTANCE,
				NotionDatabaseRowDataFetcher.INSTANCE
		);
	}
}
