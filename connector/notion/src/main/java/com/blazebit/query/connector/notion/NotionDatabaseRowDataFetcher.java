/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.notion;

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
 * Fetches all rows from every database accessible to the integration via
 * {@code POST /v1/databases/{databaseId}/query}.
 *
 * <p>This fetcher depends on {@link NotionDatabaseDataFetcher}: it retrieves the
 * database list from the session cache and issues one paginated query per database.
 *
 * <p><b>Opt-in:</b> This fetcher is disabled by default because it can be very
 * expensive in workspaces with large databases. Enable it by setting
 * {@link NotionConnectorConfig#DATABASE_ROWS_ENABLED} to {@code true} on the
 * query context.
 *
 * <p><b>Performance note:</b> One or more API calls are issued per database (pagination).
 * Restrict which databases the integration can access to limit the volume of data fetched.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class NotionDatabaseRowDataFetcher implements DataFetcher<NotionDatabaseRow>, Serializable {

	public static final NotionDatabaseRowDataFetcher INSTANCE = new NotionDatabaseRowDataFetcher();

	private NotionDatabaseRowDataFetcher() {
	}

	@Override
	public List<NotionDatabaseRow> fetch(DataFetchContext context) {
		Boolean enabled = NotionConnectorConfig.DATABASE_ROWS_ENABLED.find( context );
		if ( !Boolean.TRUE.equals( enabled ) ) {
			return List.of();
		}
		try {
			List<NotionClient> clients = NotionConnectorConfig.NOTION_CLIENT.getAll( context );
			List<? extends NotionDatabase> databases = context.getSession().getOrFetch( NotionDatabase.class );
			List<NotionDatabaseRow> list = new ArrayList<>();
			for ( NotionClient client : clients ) {
				for ( NotionDatabase database : databases ) {
					for ( JsonNode node : client.queryDatabase( database.getId() ) ) {
						list.add( NotionDatabaseRow.fromJson( node, database.getId() ) );
					}
				}
			}
			return list;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataFetcherException( "Interrupted while fetching Notion database rows", e );
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Notion database rows", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.beansConvention( NotionDatabaseRow.class );
	}
}
