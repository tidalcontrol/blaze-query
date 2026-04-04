/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a Scaleway Transactional Email (TEM) domain.
 * Used for compliance checks such as: missing SPF/DKIM/MX configuration,
 * domain health monitoring, and email delivery hygiene.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public record ScalewayTemDomain(
		String id,
		String name,
		String status,
		String region,
		String projectId,
		String organizationId,
		boolean spfConfigured,
		boolean dkimConfigured,
		boolean mxConfigured,
		long totalSent,
		long totalFailed,
		String createdAt,
		String updatedAt
) {

	public static ScalewayTemDomain from(JsonNode node) {
		boolean spfConfigured = isNonEmpty( node, "spf_config" );
		boolean dkimConfigured = isNonEmpty( node, "dkim_config" );
		boolean mxConfigured = isNonEmpty( node, "mx_blackhole" );

		JsonNode stats = node.path( "statistics" );
		long totalSent = stats.path( "total_sent" ).asLong( 0 );
		long totalFailed = stats.path( "total_failed" ).asLong( 0 );

		return new ScalewayTemDomain(
				textOrNull( node, "id" ),
				textOrNull( node, "name" ),
				textOrNull( node, "status" ),
				textOrNull( node, "region" ),
				textOrNull( node, "project_id" ),
				textOrNull( node, "organization_id" ),
				spfConfigured,
				dkimConfigured,
				mxConfigured,
				totalSent,
				totalFailed,
				textOrNull( node, "created_at" ),
				textOrNull( node, "updated_at" )
		);
	}

	private static boolean isNonEmpty(JsonNode node, String field) {
		JsonNode v = node.path( field );
		if ( v.isNull() || v.isMissingNode() ) {
			return false;
		}
		String text = v.asText( "" );
		return !text.isEmpty();
	}

	private static String textOrNull(JsonNode node, String field) {
		JsonNode v = node.path( field );
		return v.isNull() || v.isMissingNode() ? null : v.asText();
	}
}
