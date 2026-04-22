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
 * Fetches the members of every access group.
 *
 * <p>Depends on {@link AccessGroupDataFetcher} — groups are retrieved from the
 * session cache and then {@code GET /v1/access-groups/{id}/members} is called
 * for each.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class AccessGroupMemberDataFetcher implements DataFetcher<AccessGroupMember>, Serializable {

	public static final AccessGroupMemberDataFetcher INSTANCE = new AccessGroupMemberDataFetcher();

	private AccessGroupMemberDataFetcher() {
	}

	@Override
	public List<AccessGroupMember> fetch(DataFetchContext context) {
		try {
			List<VercelApiClient> apiClients = VercelConnectorConfig.API_CLIENT.getAll( context );
			List<? extends AccessGroup> groups = context.getSession().getOrFetch( AccessGroup.class );
			List<AccessGroupMember> list = new ArrayList<>();
			for ( VercelApiClient apiClient : apiClients ) {
				for ( AccessGroup group : groups ) {
					List<AccessGroupMember> members = apiClient.fetchPagedList(
							"/v1/access-groups/" + group.getAccessGroupId() + "/members",
							"members",
							AccessGroupMember.class
					);
					for ( AccessGroupMember member : members ) {
						member.setAccessGroupId( group.getAccessGroupId() );
					}
					list.addAll( members );
				}
			}
			return list;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Vercel access group member list", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.beansConvention( AccessGroupMember.class, VercelConventionContext.INSTANCE );
	}
}
