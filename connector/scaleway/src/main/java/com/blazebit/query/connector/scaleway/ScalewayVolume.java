/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a Scaleway Block Storage Volume.
 * Used for compliance checks such as: orphaned/unattached volumes,
 * volume type auditing, and storage hygiene.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public record ScalewayVolume(
		String id,
		String name,
		String volumeType,
		long size,
		String state,
		String zone,
		String projectId,
		String organizationId,
		String serverId,
		String createdAt
) {

	public static ScalewayVolume from(JsonNode node) {
		String zone = node.path( "_zone" ).isMissingNode() ? null : node.path( "_zone" ).asText();

		JsonNode serverNode = node.path( "server" );
		String serverId = null;
		if ( serverNode.isObject() && !serverNode.isMissingNode() ) {
			JsonNode serverIdNode = serverNode.path( "id" );
			if ( !serverIdNode.isNull() && !serverIdNode.isMissingNode() ) {
				serverId = serverIdNode.asText();
			}
		}

		return new ScalewayVolume(
				textOrNull( node, "id" ),
				textOrNull( node, "name" ),
				textOrNull( node, "volume_type" ),
				node.path( "size" ).asLong( 0 ),
				textOrNull( node, "state" ),
				zone,
				textOrNull( node, "project_id" ),
				textOrNull( node, "organization_id" ),
				serverId,
				textOrNull( node, "creation_date" )
		);
	}

	private static String textOrNull(JsonNode node, String field) {
		JsonNode v = node.path( field );
		return v.isNull() || v.isMissingNode() ? null : v.asText();
	}
}
