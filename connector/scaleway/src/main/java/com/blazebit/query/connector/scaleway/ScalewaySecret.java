/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Scaleway Secret Manager secret.
 * Used for compliance checks such as: detecting secrets with no active version,
 * secrets that have never been rotated, and orphaned secrets by project.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public record ScalewaySecret(
		String id,
		String name,
		String description,
		String status,
		String region,
		String projectId,
		String organizationId,
		int versionCount,
		List<String> tags,
		String createdAt,
		String updatedAt
) {

	public static ScalewaySecret from(JsonNode node) {
		return new ScalewaySecret(
				textOrNull( node, "id" ),
				textOrNull( node, "name" ),
				textOrNull( node, "description" ),
				textOrNull( node, "status" ),
				textOrNull( node, "region" ),
				textOrNull( node, "project_id" ),
				textOrNull( node, "organization_id" ),
				node.path( "version_count" ).asInt( 0 ),
				toStringList( node.path( "tags" ) ),
				textOrNull( node, "created_at" ),
				textOrNull( node, "updated_at" )
		);
	}

	private static String textOrNull(JsonNode node, String field) {
		JsonNode v = node.path( field );
		return v.isNull() || v.isMissingNode() ? null : v.asText();
	}

	private static List<String> toStringList(JsonNode array) {
		List<String> result = new ArrayList<>();
		if ( array.isArray() ) {
			for ( JsonNode item : array ) {
				result.add( item.asText() );
			}
		}
		return result;
	}
}
