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
 * Fetches all databases visible to the Notion integration via {@code POST /v1/search}.
 *
 * <p>Only databases that the integration has been granted access to are returned.
 * Results include both active and archived databases.
 *
 * @author Blazebit
 * @since 1.0.0
 */
public class NotionDatabaseDataFetcher implements DataFetcher<NotionDatabase>, Serializable {

	public static final NotionDatabaseDataFetcher INSTANCE = new NotionDatabaseDataFetcher();

	private NotionDatabaseDataFetcher() {
	}

	@Override
	public List<NotionDatabase> fetch(DataFetchContext context) {
		try {
			List<NotionClient> clients = NotionConnectorConfig.NOTION_CLIENT.getAll( context );
			List<NotionDatabase> list = new ArrayList<>();
			for ( NotionClient client : clients ) {
				for ( JsonNode node : client.searchDatabases() ) {
					list.add( NotionDatabase.fromJson( node ) );
				}
			}
			return list;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataFetcherException( "Interrupted while fetching Notion databases", e );
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Notion database list", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.beansConvention( NotionDatabase.class );
	}
}
