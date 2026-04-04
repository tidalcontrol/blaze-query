/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a Scaleway Instance security group (firewall rule set).
 * Used for network security auditing: default policies, stateful configuration,
 * SMTP blocking, and identifying overly permissive groups.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public record ScalewaySecurityGroup(
		String id,
		String name,
		String description,
		String zone,
		String organizationId,
		String projectId,
		String inboundDefaultPolicy,
		String outboundDefaultPolicy,
		boolean stateful,
		boolean enableDefaultSecurity,
		boolean organizationDefault,
		boolean projectDefault,
		String createdAt,
		String modifiedAt
) {

	public static ScalewaySecurityGroup from(JsonNode node) {
		return new ScalewaySecurityGroup(
				textOrNull( node, "id" ),
				textOrNull( node, "name" ),
				textOrNull( node, "description" ),
				textOrNull( node, "zone" ),
				textOrNull( node, "organization" ),
				textOrNull( node, "project" ),
				textOrNull( node, "inbound_default_policy" ),
				textOrNull( node, "outbound_default_policy" ),
				node.path( "stateful" ).asBoolean( true ),
				node.path( "enable_default_security" ).asBoolean( true ),
				node.path( "organization_default" ).asBoolean( false ),
				node.path( "project_default" ).asBoolean( false ),
				textOrNull( node, "creation_date" ),
				textOrNull( node, "modification_date" )
		);
	}

	private static String textOrNull(JsonNode node, String field) {
		JsonNode value = node.path( field );
		return value.isNull() || value.isMissingNode() ? null : value.asText();
	}
}
