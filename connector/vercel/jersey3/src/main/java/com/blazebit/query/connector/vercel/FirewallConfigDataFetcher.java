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
 * Fetches the WAF/firewall configuration for each project.
 *
 * <p>Depends on {@link ProjectDataFetcher} — projects are retrieved from the
 * session cache and then
 * {@code GET /v1/security/firewall/config/latest?projectIdOrName={id}}
 * is called for each.
 *
 * @author Blazebit
 * @since 1.0.0
 */
public class FirewallConfigDataFetcher implements DataFetcher<FirewallConfig>, Serializable {

	public static final FirewallConfigDataFetcher INSTANCE = new FirewallConfigDataFetcher();

	private FirewallConfigDataFetcher() {
	}

	@Override
	public List<FirewallConfig> fetch(DataFetchContext context) {
		try {
			List<VercelApiClient> apiClients = VercelConnectorConfig.API_CLIENT.getAll( context );
			List<? extends Project> projects = context.getSession().getOrFetch( Project.class );
			List<FirewallConfig> list = new ArrayList<>();
			for ( VercelApiClient apiClient : apiClients ) {
				for ( Project project : projects ) {
					try {
						FirewallConfig config = apiClient.fetchSingleWithQuery(
								"/v1/security/firewall/config/latest",
								"projectIdOrName",
								project.getId(),
								FirewallConfig.class
						);
						config.setProjectId( project.getId() );
						list.add( config );
					}
					catch (Exception e) {
						// Firewall config may not be available for all plan types — skip silently
					}
				}
			}
			return list;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Vercel firewall config list", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.beansConvention( FirewallConfig.class, VercelConventionContext.INSTANCE );
	}
}
