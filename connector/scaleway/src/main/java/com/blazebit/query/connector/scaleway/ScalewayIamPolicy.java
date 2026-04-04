/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a Scaleway IAM policy (permission set attached to users, groups, or applications).
 * Used for privilege escalation detection and least-privilege auditing.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public record ScalewayIamPolicy(
		String id,
		String name,
		String description,
		String organizationId,
		String userId,
		String groupId,
		String applicationId,
		boolean editable,
		int nbRules,
		int nbScopes,
		int nbPermissionSets,
		String createdAt,
		String updatedAt
) {

	public static ScalewayIamPolicy from(JsonNode node) {
		return new ScalewayIamPolicy(
				textOrNull( node, "id" ),
				textOrNull( node, "name" ),
				textOrNull( node, "description" ),
				textOrNull( node, "organization_id" ),
				textOrNull( node, "user_id" ),
				textOrNull( node, "group_id" ),
				textOrNull( node, "application_id" ),
				node.path( "editable" ).asBoolean( true ),
				node.path( "nb_rules" ).asInt( 0 ),
				node.path( "nb_scopes" ).asInt( 0 ),
				node.path( "nb_permission_sets" ).asInt( 0 ),
				textOrNull( node, "created_at" ),
				textOrNull( node, "updated_at" )
		);
	}

	private static String textOrNull(JsonNode node, String field) {
		JsonNode value = node.path( field );
		return value.isNull() || value.isMissingNode() ? null : value.asText();
	}
}
