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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches block children for all pages accessible to the integration via
 * {@code GET /v1/blocks/{pageId}/children}, with optional recursive traversal.
 *
 * <p>This fetcher depends on {@link NotionPageDataFetcher}: it retrieves the page list
 * from the session cache and issues one API call per page (and optionally more for
 * nested blocks).
 *
 * <p><b>Depth control:</b> By default only the direct children of each page are fetched
 * (depth 1). Set {@link NotionConnectorConfig#BLOCK_MAX_DEPTH} to a higher value to
 * traverse nested content such as toggle blocks and column layouts. Each additional level
 * of depth issues one API call per block with {@code has_children = true}.
 *
 * <p><b>Performance note:</b> API calls grow rapidly with depth in deeply nested pages.
 * Use depth &gt; 1 only when complete DLP coverage of nested content is required.
 *
 * @author Blazebit
 * @since 1.0.0
 */
public class NotionBlockDataFetcher implements DataFetcher<NotionBlock>, Serializable {

	public static final NotionBlockDataFetcher INSTANCE = new NotionBlockDataFetcher();

	private static final int DEFAULT_MAX_DEPTH = 1;

	private NotionBlockDataFetcher() {
	}

	@Override
	public List<NotionBlock> fetch(DataFetchContext context) {
		try {
			List<NotionClient> clients = NotionConnectorConfig.NOTION_CLIENT.getAll( context );
			List<? extends NotionPage> pages = context.getSession().getOrFetch( NotionPage.class );
			Integer configuredDepth = NotionConnectorConfig.BLOCK_MAX_DEPTH.find( context );
			int maxDepth = ( configuredDepth != null && configuredDepth > 0 ) ? configuredDepth : DEFAULT_MAX_DEPTH;
			List<NotionBlock> list = new ArrayList<>();
			for ( NotionClient client : clients ) {
				for ( NotionPage page : pages ) {
					fetchBlocks( client, page.getId(), page.getId(), 1, maxDepth, list );
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

	private void fetchBlocks(
			NotionClient client,
			String blockId,
			String pageId,
			int currentDepth,
			int maxDepth,
			List<NotionBlock> accumulator) throws IOException, InterruptedException {
		for ( JsonNode node : client.getBlockChildren( blockId ) ) {
			NotionBlock block = NotionBlock.fromJson( node, pageId );
			accumulator.add( block );
			if ( block.isHasChildren() && currentDepth < maxDepth ) {
				fetchBlocks( client, block.getId(), pageId, currentDepth + 1, maxDepth, accumulator );
			}
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.beansConvention( NotionBlock.class );
	}
}
