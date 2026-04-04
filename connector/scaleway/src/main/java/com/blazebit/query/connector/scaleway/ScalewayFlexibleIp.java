/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Scaleway Flexible IP address.
 * Used for compliance checks such as: unattached/orphaned IPs,
 * IP lifecycle management, and attack surface reduction.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public record ScalewayFlexibleIp(
		String id,
		String ipAddress,
		String status,
		String zone,
		String projectId,
		String organizationId,
		String serverId,
		List<String> tags,
		String createdAt,
		String updatedAt
) {

	public static ScalewayFlexibleIp from(JsonNode node) {
		JsonNode serverNode = node.path( "server" );
		String serverId = null;
		if ( serverNode.isObject() && !serverNode.isMissingNode() ) {
			JsonNode serverIdNode = serverNode.path( "id" );
			if ( !serverIdNode.isNull() && !serverIdNode.isMissingNode() ) {
				serverId = serverIdNode.asText();
			}
		}

		List<String> tags = new ArrayList<>();
		JsonNode tagsNode = node.path( "tags" );
		if ( tagsNode.isArray() ) {
			for ( JsonNode tag : tagsNode ) {
				tags.add( tag.asText() );
			}
		}

		return new ScalewayFlexibleIp(
				textOrNull( node, "id" ),
				textOrNull( node, "ip_address" ),
				textOrNull( node, "status" ),
				textOrNull( node, "zone" ),
				textOrNull( node, "project_id" ),
				textOrNull( node, "organization_id" ),
				serverId,
				tags,
				textOrNull( node, "created_at" ),
				textOrNull( node, "updated_at" )
		);
	}

	private static String textOrNull(JsonNode node, String field) {
		JsonNode v = node.path( field );
		return v.isNull() || v.isMissingNode() ? null : v.asText();
	}
}
