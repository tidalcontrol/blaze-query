/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a Scaleway security group rule (firewall rule).
 * Used for network access auditing: detecting overly permissive inbound rules,
 * unrestricted CIDR ranges (0.0.0.0/0), and open port ranges.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public record ScalewaySecurityGroupRule(
		String id,
		String securityGroupId,
		String zone,
		int position,
		String protocol,
		String direction,
		String action,
		String ipRange,
		Integer destPortFrom,
		Integer destPortTo,
		boolean editable
) {

	public static ScalewaySecurityGroupRule from(JsonNode node, String securityGroupId, String zone) {
		JsonNode portFrom = node.path( "dest_port_from" );
		JsonNode portTo = node.path( "dest_port_to" );
		return new ScalewaySecurityGroupRule(
				textOrNull( node, "id" ),
				securityGroupId,
				zone,
				node.path( "position" ).asInt( 0 ),
				textOrNull( node, "protocol" ),
				textOrNull( node, "direction" ),
				textOrNull( node, "action" ),
				textOrNull( node, "ip_range" ),
				portFrom.isNull() || portFrom.isMissingNode() ? null : portFrom.asInt(),
				portTo.isNull() || portTo.isMissingNode() ? null : portTo.asInt(),
				node.path( "editable" ).asBoolean( true )
		);
	}

	private static String textOrNull(JsonNode node, String field) {
		JsonNode value = node.path( field );
		return value.isNull() || value.isMissingNode() ? null : value.asText();
	}
}
