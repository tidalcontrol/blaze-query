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
 * Fetches all pages visible to the Notion integration via {@code POST /v1/search}.
 *
 * <p>Only pages that the integration has been granted access to are returned.
 * Results include both active and archived pages.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class NotionPageDataFetcher implements DataFetcher<NotionPage>, Serializable {

	public static final NotionPageDataFetcher INSTANCE = new NotionPageDataFetcher();

	private NotionPageDataFetcher() {
	}

	@Override
	public List<NotionPage> fetch(DataFetchContext context) {
		try {
			List<NotionClient> clients = NotionConnectorConfig.NOTION_CLIENT.getAll( context );
			List<NotionPage> list = new ArrayList<>();
			for ( NotionClient client : clients ) {
				for ( JsonNode node : client.searchPages() ) {
					list.add( NotionPage.fromJson( node ) );
				}
			}
			return list;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataFetcherException( "Interrupted while fetching Notion pages", e );
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Notion page list", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.beansConvention( NotionPage.class );
	}
}
