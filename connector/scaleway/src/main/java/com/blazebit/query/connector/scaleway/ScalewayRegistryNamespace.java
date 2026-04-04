/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a Scaleway Container Registry namespace.
 * Used for compliance checks such as: publicly accessible registries,
 * orphaned namespaces with no images, and per-project registry usage.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public record ScalewayRegistryNamespace(
		String id,
		String name,
		String description,
		String status,
		String region,
		String projectId,
		String organizationId,
		boolean publiclyAccessible,
		int imageCount,
		long size,
		String endpoint,
		String createdAt,
		String updatedAt
) {

	public static ScalewayRegistryNamespace from(JsonNode node) {
		return new ScalewayRegistryNamespace(
				textOrNull( node, "id" ),
				textOrNull( node, "name" ),
				textOrNull( node, "description" ),
				textOrNull( node, "status" ),
				textOrNull( node, "region" ),
				textOrNull( node, "project_id" ),
				textOrNull( node, "organization_id" ),
				node.path( "is_public" ).asBoolean( false ),
				node.path( "image_count" ).asInt( 0 ),
				node.path( "size" ).asLong( 0L ),
				textOrNull( node, "endpoint" ),
				textOrNull( node, "created_at" ),
				textOrNull( node, "updated_at" )
		);
	}

	private static String textOrNull(JsonNode node, String field) {
		JsonNode v = node.path( field );
		return v.isNull() || v.isMissingNode() ? null : v.asText();
	}
}
