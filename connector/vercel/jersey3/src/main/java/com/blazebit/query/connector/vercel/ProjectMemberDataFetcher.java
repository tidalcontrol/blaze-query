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
 * Fetches project-level role overrides for all members across all projects.
 *
 * <p>Depends on {@link ProjectDataFetcher} — projects are retrieved from the
 * session cache and then {@code GET /v1/projects/{id}/members} is called for each.
 *
 * <p>Project roles may be higher than the member's team role, making this
 * essential for privilege escalation audits.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class ProjectMemberDataFetcher implements DataFetcher<ProjectMember>, Serializable {

	public static final ProjectMemberDataFetcher INSTANCE = new ProjectMemberDataFetcher();

	private ProjectMemberDataFetcher() {
	}

	@Override
	public List<ProjectMember> fetch(DataFetchContext context) {
		try {
			List<VercelApiClient> apiClients = VercelConnectorConfig.API_CLIENT.getAll( context );
			List<? extends Project> projects = context.getSession().getOrFetch( Project.class );
			List<ProjectMember> list = new ArrayList<>();
			for ( VercelApiClient apiClient : apiClients ) {
				for ( Project project : projects ) {
					List<ProjectMember> members = apiClient.fetchPagedList(
							"/v1/projects/" + project.getId() + "/members",
							"members",
							ProjectMember.class
					);
					for ( ProjectMember member : members ) {
						member.setProjectId( project.getId() );
					}
					list.addAll( members );
				}
			}
			return list;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Vercel project member list", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.beansConvention( ProjectMember.class, VercelConventionContext.INSTANCE );
	}
}
