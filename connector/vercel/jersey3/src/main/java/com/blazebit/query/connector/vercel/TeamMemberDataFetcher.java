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
 * Fetches all members of each accessible Vercel team.
 *
 * <p>Depends on {@link TeamDataFetcher} — teams are retrieved from the session cache
 * and then {@code GET /v3/teams/{teamId}/members} is called for each.
 *
 * <p>Useful for auditing who has access to each team, what roles they hold,
 * and how they joined (SAML, directory sync, invitation, etc.).
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class TeamMemberDataFetcher implements DataFetcher<TeamMember>, Serializable {

	public static final TeamMemberDataFetcher INSTANCE = new TeamMemberDataFetcher();

	private TeamMemberDataFetcher() {
	}

	@Override
	public List<TeamMember> fetch(DataFetchContext context) {
		try {
			List<VercelApiClient> apiClients = VercelConnectorConfig.API_CLIENT.getAll( context );
			List<? extends Team> teams = context.getSession().getOrFetch( Team.class );
			List<TeamMember> list = new ArrayList<>();
			for ( VercelApiClient apiClient : apiClients ) {
				for ( Team team : teams ) {
					String path = "/v3/teams/" + team.getId() + "/members";
					List<TeamMember> members = apiClient.fetchPagedListByPath(
							path, "members", TeamMember.class );
					for ( TeamMember member : members ) {
						member.setTeamId( team.getId() );
					}
					list.addAll( members );
				}
			}
			return list;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Vercel team member list", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.beansConvention( TeamMember.class, VercelConventionContext.INSTANCE );
	}
}
