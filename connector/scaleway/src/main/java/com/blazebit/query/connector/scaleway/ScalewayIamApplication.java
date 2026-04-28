/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a Scaleway IAM application (non-human/service principal).
 * Used for auditing service account permissions and API key ownership.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public record ScalewayIamApplication(
		String id,
		String name,
		String description,
		String organizationId,
		boolean editable,
		int nbApiKeys,
		String createdAt,
		String updatedAt
) {

	public static ScalewayIamApplication from(JsonNode node) {
		return new ScalewayIamApplication(
				textOrNull( node, "id" ),
				textOrNull( node, "name" ),
				textOrNull( node, "description" ),
				textOrNull( node, "organization_id" ),
				node.path( "editable" ).asBoolean( true ),
				node.path( "nb_api_keys" ).asInt( 0 ),
				textOrNull( node, "created_at" ),
				textOrNull( node, "updated_at" )
		);
	}

	private static String textOrNull(JsonNode node, String field) {
		JsonNode value = node.path( field );
		return value.isNull() || value.isMissingNode() ? null : value.asText();
	}
}
