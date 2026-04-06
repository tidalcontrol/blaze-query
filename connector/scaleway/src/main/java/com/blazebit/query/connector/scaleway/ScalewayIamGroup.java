/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Scaleway IAM group (a collection of users and/or applications).
 * Used for access control auditing and least-privilege analysis.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public record ScalewayIamGroup(
		String id,
		String name,
		String description,
		String organizationId,
		List<String> userIds,
		List<String> applicationIds,
		String createdAt,
		String updatedAt
) {

	public static ScalewayIamGroup from(JsonNode node) {
		return new ScalewayIamGroup(
				textOrNull( node, "id" ),
				textOrNull( node, "name" ),
				textOrNull( node, "description" ),
				textOrNull( node, "organization_id" ),
				toStringList( node.path( "user_ids" ) ),
				toStringList( node.path( "application_ids" ) ),
				textOrNull( node, "created_at" ),
				textOrNull( node, "updated_at" )
		);
	}

	private static String textOrNull(JsonNode node, String field) {
		JsonNode value = node.path( field );
		return value.isNull() || value.isMissingNode() ? null : value.asText();
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
