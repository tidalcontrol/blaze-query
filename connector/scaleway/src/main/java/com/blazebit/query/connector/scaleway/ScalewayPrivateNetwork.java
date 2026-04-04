/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Scaleway Private Network within a VPC.
 * Used for network isolation auditing: verifying that compute resources
 * are deployed within private networks and not solely on public interfaces.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public record ScalewayPrivateNetwork(
		String id,
		String name,
		String vpcId,
		String region,
		String projectId,
		String organizationId,
		List<String> subnets,
		List<String> tags,
		String createdAt,
		String updatedAt
) {

	public static ScalewayPrivateNetwork from(JsonNode node) {
		return new ScalewayPrivateNetwork(
				textOrNull( node, "id" ),
				textOrNull( node, "name" ),
				textOrNull( node, "vpc_id" ),
				textOrNull( node, "region" ),
				textOrNull( node, "project_id" ),
				textOrNull( node, "organization_id" ),
				toStringList( node.path( "subnets" ) ),
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
				result.add( item.isObject() ? item.toString() : item.asText() );
			}
		}
		return result;
	}
}
