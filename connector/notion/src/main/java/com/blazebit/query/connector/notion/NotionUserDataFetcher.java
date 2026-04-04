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
 * Fetches all users visible to the Notion integration token via {@code GET /v1/users}.
 *
 * <p>The result includes both human workspace members ({@code type = "person"}) and
 * bot integrations ({@code type = "bot"}). A workspace-level integration token is
 * required to list all users; user-level tokens only return the token owner.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class NotionUserDataFetcher implements DataFetcher<NotionUser>, Serializable {

	public static final NotionUserDataFetcher INSTANCE = new NotionUserDataFetcher();

	private NotionUserDataFetcher() {
	}

	@Override
	public List<NotionUser> fetch(DataFetchContext context) {
		try {
			List<NotionClient> clients = NotionConnectorConfig.NOTION_CLIENT.getAll( context );
			List<NotionUser> list = new ArrayList<>();
			for ( NotionClient client : clients ) {
				for ( JsonNode node : client.listUsers() ) {
					list.add( NotionUser.fromJson( node ) );
				}
			}
			return list;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataFetcherException( "Interrupted while fetching Notion users", e );
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Notion user list", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.beansConvention( NotionUser.class );
	}
}
