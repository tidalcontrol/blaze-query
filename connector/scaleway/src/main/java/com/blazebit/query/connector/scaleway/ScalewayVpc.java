/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Scaleway VPC (Virtual Private Cloud).
 * Used for network segmentation auditing: detecting default VPCs with routing
 * enabled (which allows all private networks to communicate, a lateral movement risk).
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public record ScalewayVpc(
		String id,
		String name,
		String region,
		String projectId,
		String organizationId,
		boolean defaultVpc,
		boolean routingEnabled,
		List<String> tags,
		String createdAt,
		String updatedAt
) {

	public static ScalewayVpc from(JsonNode node) {
		return new ScalewayVpc(
				textOrNull( node, "id" ),
				textOrNull( node, "name" ),
				textOrNull( node, "region" ),
				textOrNull( node, "project_id" ),
				textOrNull( node, "organization_id" ),
				node.path( "is_default" ).asBoolean( false ),
				node.path( "routing_enabled" ).asBoolean( false ),
				toStringList( node.path( "tags" ) ),
				textOrNull( node, "created_at" ),
				textOrNull( node, "updated_at" )
		);
	}

	private static String textOrNull(JsonNode node, String field) {
		JsonNode v = node.path( field );
		return v.isNull() || v.isMissingNode() ? null : v.asText();
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
