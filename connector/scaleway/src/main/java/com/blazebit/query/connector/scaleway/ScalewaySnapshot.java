/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Scaleway Instance Snapshot.
 * Used for compliance checks such as: orphaned snapshots,
 * snapshot age auditing, and storage hygiene.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public record ScalewaySnapshot(
		String id,
		String name,
		String state,
		long size,
		String volumeType,
		String zone,
		String projectId,
		String organizationId,
		List<String> tags,
		String createdAt
) {

	public static ScalewaySnapshot from(JsonNode node) {
		String zone = node.path( "_zone" ).isMissingNode() ? null : node.path( "_zone" ).asText();

		List<String> tags = new ArrayList<>();
		JsonNode tagsNode = node.path( "tags" );
		if ( tagsNode.isArray() ) {
			for ( JsonNode tag : tagsNode ) {
				tags.add( tag.asText() );
			}
		}

		return new ScalewaySnapshot(
				textOrNull( node, "id" ),
				textOrNull( node, "name" ),
				textOrNull( node, "state" ),
				node.path( "size" ).asLong( 0 ),
				textOrNull( node, "volume_type" ),
				zone,
				textOrNull( node, "project_id" ),
				textOrNull( node, "organization_id" ),
				tags,
				textOrNull( node, "creation_date" )
		);
	}

	private static String textOrNull(JsonNode node, String field) {
		JsonNode v = node.path( field );
		return v.isNull() || v.isMissingNode() ? null : v.asText();
	}
}
