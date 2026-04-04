/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a Scaleway Key Manager (KMS) encryption key.
 * Used for compliance checks such as: keys without a rotation policy,
 * keys past their scheduled rotation date, and disabled/locked keys.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public record ScalewayKmsKey(
		String id,
		String name,
		String description,
		String state,
		String algorithm,
		String region,
		String projectId,
		String organizationId,
		String rotationPeriod,
		String nextRotationAt,
		String lastRotatedAt,
		boolean rotationEnabled,
		String createdAt,
		String updatedAt
) {

	public static ScalewayKmsKey from(JsonNode node) {
		JsonNode rotationPolicy = node.path( "rotation_policy" );
		String rotationPeriod = null;
		boolean rotationEnabled = false;
		if ( rotationPolicy.isObject() && !rotationPolicy.isMissingNode() ) {
			rotationPeriod = textOrNull( rotationPolicy, "rotation_period" );
			rotationEnabled = rotationPeriod != null;
		}
		return new ScalewayKmsKey(
				textOrNull( node, "id" ),
				textOrNull( node, "name" ),
				textOrNull( node, "description" ),
				textOrNull( node, "state" ),
				textOrNull( node, "algorithm" ),
				textOrNull( node, "region" ),
				textOrNull( node, "project_id" ),
				textOrNull( node, "organization_id" ),
				rotationPeriod,
				textOrNull( node, "next_rotation_at" ),
				textOrNull( node, "last_rotated_at" ),
				rotationEnabled,
				textOrNull( node, "created_at" ),
				textOrNull( node, "updated_at" )
		);
	}

	private static String textOrNull(JsonNode node, String field) {
		JsonNode v = node.path( field );
		return v.isNull() || v.isMissingNode() ? null : v.asText();
	}
}
