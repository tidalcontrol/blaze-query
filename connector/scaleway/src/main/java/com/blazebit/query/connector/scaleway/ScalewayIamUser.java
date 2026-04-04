/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a Scaleway IAM user. Used for compliance checks such as
 * MFA enforcement, account status, and user type classification.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public record ScalewayIamUser(
		String id,
		String email,
		String organizationId,
		String status,
		boolean mfa,
		String type,
		String createdAt,
		String updatedAt,
		String lastLoginAt
) {

	public static ScalewayIamUser from(JsonNode node) {
		return new ScalewayIamUser(
				textOrNull( node, "id" ),
				textOrNull( node, "email" ),
				textOrNull( node, "organization_id" ),
				textOrNull( node, "status" ),
				node.path( "mfa" ).asBoolean( false ),
				textOrNull( node, "type" ),
				textOrNull( node, "created_at" ),
				textOrNull( node, "updated_at" ),
				textOrNull( node, "last_login_at" )
		);
	}

	private static String textOrNull(JsonNode node, String field) {
		JsonNode value = node.path( field );
		return value.isNull() || value.isMissingNode() ? null : value.asText();
	}
}
