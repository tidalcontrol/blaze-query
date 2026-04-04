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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches workspace identity information for each configured Notion integration via
 * {@code GET /v1/users/me}.
 *
 * <p>Each configured {@link NotionClient} contributes one row, representing the workspace
 * the integration is installed in. In a multi-workspace setup (multiple clients configured)
 * this produces one row per workspace.
 *
 * <p><b>Note:</b> Workspace-level security settings (SAML SSO, SCIM, domain verification,
 * audit logs) are not exposed by the Notion public REST API. They require either the
 * Notion admin UI or admission to Notion's Security &amp; Compliance partner programme.
 *
 * @author Blazebit
 * @since 1.0.0
 */
public class NotionWorkspaceDataFetcher implements DataFetcher<NotionWorkspace>, Serializable {

	public static final NotionWorkspaceDataFetcher INSTANCE = new NotionWorkspaceDataFetcher();

	private NotionWorkspaceDataFetcher() {
	}

	@Override
	public List<NotionWorkspace> fetch(DataFetchContext context) {
		try {
			List<NotionClient> clients = NotionConnectorConfig.NOTION_CLIENT.getAll( context );
			List<NotionWorkspace> list = new ArrayList<>();
			for ( NotionClient client : clients ) {
				list.add( NotionWorkspace.fromJson( client.getMe() ) );
			}
			return list;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DataFetcherException( "Interrupted while fetching Notion workspace info", e );
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Notion workspace info", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.beansConvention( NotionWorkspace.class );
	}
}
