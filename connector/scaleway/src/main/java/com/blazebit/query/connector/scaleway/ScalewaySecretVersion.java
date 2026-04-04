/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a version of a Scaleway Secret Manager secret.
 * Used for rotation auditing: detecting secrets whose latest version
 * is very old (indicating no rotation has occurred).
 *
 * @author Blazebit
 * @since 2.4.4
 */
public record ScalewaySecretVersion(
		String secretId,
		String revision,
		String status,
		String region,
		String createdAt,
		String updatedAt
) {

	public static ScalewaySecretVersion from(JsonNode node, String secretId, String region) {
		return new ScalewaySecretVersion(
				secretId,
				textOrNull( node, "revision" ),
				textOrNull( node, "status" ),
				region,
				textOrNull( node, "created_at" ),
				textOrNull( node, "updated_at" )
		);
	}

	private static String textOrNull(JsonNode node, String field) {
		JsonNode v = node.path( field );
		return v.isNull() || v.isMissingNode() ? null : v.asText();
	}
}
