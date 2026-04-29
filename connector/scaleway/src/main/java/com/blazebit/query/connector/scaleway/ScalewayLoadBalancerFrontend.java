/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a Scaleway Load Balancer Frontend.
 * Used for compliance checks such as: TLS enforcement on frontends,
 * unencrypted HTTP endpoints, and certificate management.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public record ScalewayLoadBalancerFrontend(
		String id,
		String name,
		String lbId,
		String zone,
		String protocol,
		int inboundPort,
		boolean tlsEnabled,
		String createdAt,
		String updatedAt
) {

	public static ScalewayLoadBalancerFrontend from(JsonNode node, String lbId, String zone) {
		String protocol = textOrNull( node, "inbound_protocol" );
		if ( protocol == null ) {
			protocol = textOrNull( node, "protocol" );
		}

		boolean tlsEnabled = false;
		if ( "https".equals( protocol ) ) {
			tlsEnabled = true;
		}
		else {
			JsonNode certIds = node.path( "certificate_ids" );
			if ( certIds.isArray() && certIds.size() > 0 ) {
				tlsEnabled = true;
			}
		}

		return new ScalewayLoadBalancerFrontend(
				textOrNull( node, "id" ),
				textOrNull( node, "name" ),
				lbId,
				zone,
				protocol,
				node.path( "inbound_port" ).asInt( 0 ),
				tlsEnabled,
				textOrNull( node, "created_at" ),
				textOrNull( node, "updated_at" )
		);
	}

	private static String textOrNull(JsonNode node, String field) {
		JsonNode v = node.path( field );
		return v.isNull() || v.isMissingNode() ? null : v.asText();
	}
}
