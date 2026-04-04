/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a Scaleway IAM SSH key. Used for auditing access credentials:
 * disabled keys, stale keys, and per-project key assignment.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public record ScalewayIamSshKey(
		String id,
		String name,
		String publicKey,
		String fingerprint,
		boolean disabled,
		String organizationId,
		String projectId,
		String createdAt,
		String updatedAt
) {

	public static ScalewayIamSshKey from(JsonNode node) {
		return new ScalewayIamSshKey(
				textOrNull( node, "id" ),
				textOrNull( node, "name" ),
				textOrNull( node, "public_key" ),
				textOrNull( node, "fingerprint" ),
				node.path( "disabled" ).asBoolean( false ),
				textOrNull( node, "organization_id" ),
				textOrNull( node, "project_id" ),
				textOrNull( node, "created_at" ),
				textOrNull( node, "updated_at" )
		);
	}

	private static String textOrNull(JsonNode node, String field) {
		JsonNode value = node.path( field );
		return value.isNull() || value.isMissingNode() ? null : value.asText();
	}
}
