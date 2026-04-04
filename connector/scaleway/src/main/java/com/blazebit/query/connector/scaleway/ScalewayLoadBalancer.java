/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Scaleway Load Balancer.
 * Used for compliance checks such as: unencrypted frontends,
 * TLS enforcement, and load balancer security posture.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public record ScalewayLoadBalancer(
		String id,
		String name,
		String status,
		String zone,
		String projectId,
		String organizationId,
		List<String> tags,
		String createdAt,
		String updatedAt
) {

	public static ScalewayLoadBalancer from(JsonNode node) {
		List<String> tags = new ArrayList<>();
		JsonNode tagsNode = node.path( "tags" );
		if ( tagsNode.isArray() ) {
			for ( JsonNode tag : tagsNode ) {
				tags.add( tag.asText() );
			}
		}

		return new ScalewayLoadBalancer(
				textOrNull( node, "id" ),
				textOrNull( node, "name" ),
				textOrNull( node, "status" ),
				textOrNull( node, "zone" ),
				textOrNull( node, "project_id" ),
				textOrNull( node, "organization_id" ),
				tags,
				textOrNull( node, "created_at" ),
				textOrNull( node, "updated_at" )
		);
	}

	private static String textOrNull(JsonNode node, String field) {
		JsonNode v = node.path( field );
		return v.isNull() || v.isMissingNode() ? null : v.asText();
	}
}
