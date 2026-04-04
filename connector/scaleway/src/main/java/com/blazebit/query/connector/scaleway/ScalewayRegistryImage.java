/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a container image in a Scaleway Container Registry namespace.
 * Used for compliance checks such as: publicly visible images,
 * images with no tags (untagged/dangling), and stale images.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public record ScalewayRegistryImage(
		String id,
		String name,
		String namespaceId,
		String status,
		String visibility,
		String region,
		long size,
		List<String> tags,
		String createdAt,
		String updatedAt
) {

	public static ScalewayRegistryImage from(JsonNode node) {
		return new ScalewayRegistryImage(
				textOrNull( node, "id" ),
				textOrNull( node, "name" ),
				textOrNull( node, "namespace_id" ),
				textOrNull( node, "status" ),
				textOrNull( node, "visibility" ),
				textOrNull( node, "region" ),
				node.path( "size" ).asLong( 0L ),
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
