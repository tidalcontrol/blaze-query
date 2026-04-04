/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Scaleway Kubernetes Kapsule cluster.
 * Used for compliance checks such as: outdated Kubernetes versions,
 * clusters with upgrades available, public API server exposure,
 * and clusters not connected to a private network.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public record ScalewayK8sCluster(
		String id,
		String name,
		String status,
		String version,
		String cni,
		String region,
		String projectId,
		String organizationId,
		boolean upgradeAvailable,
		boolean privateNetworkEnabled,
		String privateNetworkId,
		List<String> tags,
		String createdAt,
		String updatedAt
) {

	public static ScalewayK8sCluster from(JsonNode node) {
		JsonNode privateNetwork = node.path( "private_network_id" );
		String privateNetworkId = privateNetwork.isNull() || privateNetwork.isMissingNode()
				? null : privateNetwork.asText();
		return new ScalewayK8sCluster(
				textOrNull( node, "id" ),
				textOrNull( node, "name" ),
				textOrNull( node, "status" ),
				textOrNull( node, "version" ),
				textOrNull( node, "cni" ),
				textOrNull( node, "region" ),
				textOrNull( node, "project_id" ),
				textOrNull( node, "organization_id" ),
				node.path( "upgrade_available" ).asBoolean( false ),
				privateNetworkId != null,
				privateNetworkId,
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
