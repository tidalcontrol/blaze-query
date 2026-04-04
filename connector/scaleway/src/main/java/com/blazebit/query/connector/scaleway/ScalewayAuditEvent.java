/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a Scaleway Audit Trail event.
 * Used for security investigations and compliance reporting: detecting forbidden
 * access attempts, privilege escalation activity, and unusual API usage patterns.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public record ScalewayAuditEvent(
		String id,
		String recordedAt,
		String principalId,
		String principalType,
		String sourceIp,
		String userAgent,
		String apiMethod,
		String status,
		String resourceType,
		String resourceId,
		String projectId,
		String organizationId,
		String region,
		String locality
) {

	public static ScalewayAuditEvent from(JsonNode node) {
		JsonNode principal = node.path( "principal" );
		String principalId = null;
		String principalType = null;
		if ( principal.isObject() ) {
			principalId = textOrNull( principal, "id" );
			principalType = textOrNull( principal, "type" );
		}
		JsonNode resource = node.path( "resource" );
		String resourceType = null;
		String resourceId = null;
		if ( resource.isObject() ) {
			resourceType = textOrNull( resource, "type" );
			resourceId = textOrNull( resource, "id" );
		}
		return new ScalewayAuditEvent(
				textOrNull( node, "id" ),
				textOrNull( node, "timestamp" ),
				principalId,
				principalType,
				textOrNull( node, "source_ip" ),
				textOrNull( node, "user_agent" ),
				textOrNull( node, "method" ),
				textOrNull( node, "status" ),
				resourceType,
				resourceId,
				textOrNull( node, "project_id" ),
				textOrNull( node, "organization_id" ),
				textOrNull( node, "region" ),
				textOrNull( node, "locality" )
		);
	}

	private static String textOrNull(JsonNode node, String field) {
		JsonNode v = node.path( field );
		return v.isNull() || v.isMissingNode() ? null : v.asText();
	}
}
