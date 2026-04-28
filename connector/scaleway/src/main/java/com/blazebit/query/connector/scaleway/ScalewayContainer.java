/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a Scaleway Serverless Container.
 * Used for compliance checks such as: public containers,
 * missing env-var secrets detection, and scaling configuration.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public record ScalewayContainer(
		String id,
		String name,
		String namespaceId,
		String status,
		String region,
		String privacy,
		String protocol,
		int cpuLimit,
		int memoryLimit,
		int minScale,
		int maxScale,
		boolean hasEnvVars,
		String createdAt,
		String updatedAt
) {

	public static ScalewayContainer from(JsonNode node) {
		JsonNode envVars = node.path( "environment_variables" );
		boolean hasEnvVars = envVars.isObject() && envVars.size() > 0;

		return new ScalewayContainer(
				textOrNull( node, "id" ),
				textOrNull( node, "name" ),
				textOrNull( node, "namespace_id" ),
				textOrNull( node, "status" ),
				textOrNull( node, "region" ),
				textOrNull( node, "privacy" ),
				textOrNull( node, "protocol" ),
				node.path( "cpu_limit" ).asInt( 0 ),
				node.path( "memory_limit" ).asInt( 0 ),
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
