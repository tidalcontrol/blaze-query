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
 * Fetches the top-level block children for all pages accessible to the integration
 * via {@code GET /v1/blocks/{pageId}/children}.
 *
 * <p>This fetcher depends on {@link NotionPageDataFetcher}: it first retrieves the
 * full page list from the session cache and then issues one API call per page to load
 * its direct block children. Only the first level of the block tree is fetched; use
 * {@link NotionClient#getBlockChildren(String)} directly to retrieve deeper nesting.
 *
 * <p><b>Performance note:</b> The number of API calls is proportional to the number of
 * pages the integration can access. In large workspaces, pre-filter with a SQL
 * {@code WHERE pageId = ?} predicate or limit the pages shared with the integration to
 * reduce load.
 *
 * @author Blazebit
 * @since 1.0.0
 */
public class NotionBlockDataFetcher implements DataFetcher<NotionBlock>, Serializable {

	public static final NotionBlockDataFetcher INSTANCE = new NotionBlockDataFetcher();

	private NotionBlockDataFetcher() {
	}

	@Override
	public List<NotionBlock> fetch(DataFetchContext context) {
		try {
			List<NotionClient> clients = NotionConnectorConfig.NOTION_CLIENT.getAll( context );
			List<? extends NotionPage> pages = context.getSession().getOrFetch( NotionPage.class );
			List<NotionBlock> list = new ArrayList<>();
			for ( NotionClient client : clients ) {
				for ( NotionPage page : pages ) {
					for ( JsonNode node : client.getBlockChildren( page.getId() ) ) {
						list.add( NotionBlock.fromJson( node, page.getId() ) );
					}
				}
			}
			return list;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataFetcherException( "Interrupted while fetching Notion blocks", e );
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Notion block list", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.beansConvention( NotionBlock.class );
	}
}
