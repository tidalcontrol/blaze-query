/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a Scaleway IAM API key (access key + secret key pair).
 * Used for auditing credential hygiene: expiry, unused keys, and key ownership.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public record ScalewayIamApiKey(
		String accessKey,
		String applicationId,
		String userId,
		String description,
		boolean editable,
		String createdAt,
		String expiresAt,
		String defaultProjectId
) {

	public static ScalewayIamApiKey from(JsonNode node) {
		return new ScalewayIamApiKey(
				textOrNull( node, "access_key" ),
				textOrNull( node, "application_id" ),
				textOrNull( node, "user_id" ),
				textOrNull( node, "description" ),
				node.path( "editable" ).asBoolean( true ),
				textOrNull( node, "created_at" ),
				textOrNull( node, "expires_at" ),
				textOrNull( node, "default_project_id" )
		);
	}

	private static String textOrNull(JsonNode node, String field) {
		JsonNode value = node.path( field );
		return value.isNull() || value.isMissingNode() ? null : value.asText();
	}
}
