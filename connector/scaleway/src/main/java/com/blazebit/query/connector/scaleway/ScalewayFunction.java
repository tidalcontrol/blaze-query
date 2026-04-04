/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a Scaleway Serverless Function.
 * Used for compliance checks such as: public functions,
 * missing env-var secrets detection, and runtime version auditing.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public record ScalewayFunction(
		String id,
		String name,
		String namespaceId,
		String status,
		String region,
		String runtime,
		String privacy,
		int memoryLimit,
		int cpuLimit,
		int minScale,
		int maxScale,
		boolean hasEnvVars,
		String createdAt,
		String updatedAt
) {

	public static ScalewayFunction from(JsonNode node) {
		JsonNode envVars = node.path( "environment_variables" );
		boolean hasEnvVars = envVars.isObject() && envVars.size() > 0;

		return new ScalewayFunction(
				textOrNull( node, "id" ),
				textOrNull( node, "name" ),
				textOrNull( node, "namespace_id" ),
				textOrNull( node, "status" ),
				textOrNull( node, "region" ),
				textOrNull( node, "runtime" ),
				textOrNull( node, "privacy" ),
				node.path( "memory_limit" ).asInt( 0 ),
				node.path( "cpu_limit" ).asInt( 0 ),
				node.path( "min_scale" ).asInt( 0 ),
				node.path( "max_scale" ).asInt( 0 ),
				hasEnvVars,
				textOrNull( node, "created_at" ),
				textOrNull( node, "updated_at" )
		);
	}

	private static String textOrNull(JsonNode node, String field) {
		JsonNode v = node.path( field );
		return v.isNull() || v.isMissingNode() ? null : v.asText();
	}
}
