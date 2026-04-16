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
 * Fetches all comments for every page accessible to the integration via
 * {@code GET /v1/comments?block_id={pageId}}.
 *
 * <p>This fetcher depends on {@link NotionPageDataFetcher}: it retrieves the page list
 * from the session cache and issues one API call per page to load its comments.
 * Both top-level page comments and block-anchored inline comments are returned.
 *
 * <p><b>Performance note:</b> One API call is issued per accessible page. Limit the
 * pages the integration can access to reduce load in large workspaces.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class NotionCommentDataFetcher implements DataFetcher<NotionComment>, Serializable {

	public static final NotionCommentDataFetcher INSTANCE = new NotionCommentDataFetcher();

	private NotionCommentDataFetcher() {
	}

	@Override
	public List<NotionComment> fetch(DataFetchContext context) {
		try {
			// Use the first client — the page list from the session cache already
			// contains pages fetched by all configured clients.
			List<NotionClient> clients = NotionConnectorConfig.NOTION_CLIENT.getAll( context );
			NotionClient client = clients.get( 0 );
			List<? extends NotionPage> pages = context.getSession().getOrFetch( NotionPage.class );
			List<NotionComment> list = new ArrayList<>();
			for ( NotionPage page : pages ) {
				for ( JsonNode node : client.listComments( page.getId() ) ) {
					list.add( NotionComment.fromJson( node, page.getId() ) );
				}
			}
			return list;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataFetcherException( "Interrupted while fetching Notion comments", e );
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Notion comment list", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.beansConvention( NotionComment.class );
	}
}
