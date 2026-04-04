/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Scaleway Compute Instance (server).
 * Used for infrastructure auditing: running state, public exposure, security group assignment,
 * IPv6 enablement, and deletion protection.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public record ScalewayInstance(
		String id,
		String name,
		String state,
		String commercialType,
		String arch,
		String hostname,
		String privateIp,
		String publicIp,
		boolean enableIpv6,
		boolean dynamicIpRequired,
		boolean instanceProtected,
		List<String> tags,
		String zone,
		String organizationId,
		String projectId,
		String imageId,
		String imageName,
		String securityGroupId,
		String securityGroupName,
		String createdAt,
		String modifiedAt
) {

	public static ScalewayInstance from(JsonNode node) {
		JsonNode publicIpNode = node.path( "public_ip" );
		String publicIp = publicIpNode.isObject() ? textOrNull( publicIpNode, "address" ) : null;

		JsonNode imageNode = node.path( "image" );
		String imageId = imageNode.isObject() ? textOrNull( imageNode, "id" ) : null;
		String imageName = imageNode.isObject() ? textOrNull( imageNode, "name" ) : null;

		JsonNode sgNode = node.path( "security_group" );
		String sgId = sgNode.isObject() ? textOrNull( sgNode, "id" ) : null;
		String sgName = sgNode.isObject() ? textOrNull( sgNode, "name" ) : null;

		return new ScalewayInstance(
				textOrNull( node, "id" ),
				textOrNull( node, "name" ),
				textOrNull( node, "state" ),
				textOrNull( node, "commercial_type" ),
				textOrNull( node, "arch" ),
				textOrNull( node, "hostname" ),
				textOrNull( node, "private_ip" ),
				publicIp,
				node.path( "enable_ipv6" ).asBoolean( false ),
				node.path( "dynamic_ip_required" ).asBoolean( false ),
				node.path( "protected" ).asBoolean( false ),
				toStringList( node.path( "tags" ) ),
				textOrNull( node, "zone" ),
				textOrNull( node, "organization" ),
				textOrNull( node, "project" ),
				imageId,
				imageName,
				sgId,
				sgName,
				textOrNull( node, "creation_date" ),
				textOrNull( node, "modification_date" )
		);
	}

	private static String textOrNull(JsonNode node, String field) {
		JsonNode value = node.path( field );
		return value.isNull() || value.isMissingNode() ? null : value.asText();
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
