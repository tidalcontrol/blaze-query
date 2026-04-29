/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

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
 * Fetches Scaleway security group rules for network access auditing:
 * unrestricted CIDR ranges (0.0.0.0/0), overly permissive inbound rules,
 * and wide port range exposure.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class ScalewaySecurityGroupRuleDataFetcher implements DataFetcher<ScalewaySecurityGroupRule>, Serializable {

	public static final ScalewaySecurityGroupRuleDataFetcher INSTANCE = new ScalewaySecurityGroupRuleDataFetcher();

	private ScalewaySecurityGroupRuleDataFetcher() {
	}

	@Override
	public List<ScalewaySecurityGroupRule> fetch(DataFetchContext context) {
		try {
			List<ScalewayClient> clients = ScalewayConnectorConfig.SCALEWAY_CLIENT.getAll( context );
			List<ScalewaySecurityGroupRule> result = new ArrayList<>();
			for ( ScalewayClient client : clients ) {
				List<JsonNode> groups = client.listSecurityGroups();
				for ( JsonNode group : groups ) {
					String groupId = group.path( "id" ).asText( null );
					String zone = group.path( "zone" ).asText( null );
					List<JsonNode> rules = client.listSecurityGroupRuleNodes( groupId, zone );
					for ( JsonNode rule : rules ) {
						result.add( ScalewaySecurityGroupRule.from( rule, groupId, zone ) );
					}
				}
			}
			return result;
		}
		catch (Exception e) {
			throw new DataFetcherException( "Could not fetch Scaleway security group rules", e );
		}
	}

	@Override
	public DataFormat getDataFormat() {
		return DataFormats.componentMethodConvention( ScalewaySecurityGroupRule.class, ScalewayConventionContext.INSTANCE );
	}
}
