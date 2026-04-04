/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Scaleway Object Storage bucket.
 * Used for compliance checks such as: public bucket exposure,
 * orphaned/empty buckets, and data volume hygiene.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public record ScalewayObjectStorageBucket(
		String name,
		String region,
		String projectId,
		String organizationId,
		long objectCount,
		long totalSize,
		List<String> tags,
		String createdAt,
		String updatedAt
) {

	public static ScalewayObjectStorageBucket from(JsonNode node) {
		JsonNode stats = node.path( "statistics" );
		long objectCount = stats.path( "objects_count" ).asLong( 0 );
		long totalSize = stats.path( "objects_total_size" ).asLong( 0 );

		List<String> tags = new ArrayList<>();
		JsonNode tagsNode = node.path( "tags" );
		if ( tagsNode.isArray() ) {
			for ( JsonNode tag : tagsNode ) {
				tags.add( tag.asText() );
			}
		}

		return new ScalewayObjectStorageBucket(
				textOrNull( node, "name" ),
				textOrNull( node, "region" ),
				textOrNull( node, "project_id" ),
				textOrNull( node, "organization_id" ),
				objectCount,
				totalSize,
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
