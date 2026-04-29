/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.vercel;

import com.blazebit.query.connector.base.DataFormats;
import com.blazebit.query.spi.DataFetchContext;
import com.blazebit.query.spi.DataFetcher;
import com.blazebit.query.spi.DataFetcherException;
import com.blazebit.query.spi.DataFormat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches Vercel team metadata including security and compliance settings.
 *
 * <p>If the configured {@link VercelApiClient} has a {@code teamId}, fetches that
 * single team via {@code GET /v2/teams/{teamId}}.  Otherwise, lists all accessible
 * teams via {@code GET /v2/teams}.
 *
 * <p>The returned {@link Team} objects are cached in the session and reused by
 * {@link TeamMemberDataFetcher}.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class TeamDataFetcher implements DataFetcher<Team>, Serializable {

	public static final TeamDataFetcher INSTANCE = new TeamDataFetcher();

	private TeamDataFetcher() {
	}

	@Override
	public List<Team> fetch(DataFetchContext context) {
		try {
			List<VercelApiClient> apiClients = VercelConnectorConfig.API_CLIENT.getAll( context );
			List<Team> list = new ArrayList<>();
			for ( VercelApiClient apiClient : apiClients ) {
				if ( apiClient.getTeamId() != null ) {
					list.add( apiClient.fetchSingle( "/v2/teams/" + apiClient.getTeamId(), Team.class ) );
				}
				else {
					list.addAll( apiClient.fetchPagedList( "/v2/teams", "teams", Team.class ) );
				}
			}
			return list;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Vercel team list", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.beansConvention( Team.class, VercelConventionContext.INSTANCE );
	}
}
