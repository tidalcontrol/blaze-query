/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Scaleway Managed Database (RDB) instance.
 * Used for compliance checks such as: publicly accessible endpoints,
 * missing HA configuration, and insufficient backup retention.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public record ScalewayDatabase(
		String id,
		String name,
		String status,
		String engine,
		String volumeType,
		String region,
		String projectId,
		String organizationId,
		boolean haEnabled,
		boolean publiclyAccessible,
		int backupRetentionDays,
		String endpointIp,
		List<String> tags,
		String createdAt,
		String updatedAt
) {

	public static ScalewayDatabase from(JsonNode node) {
		boolean haEnabled = node.path( "is_ha_cluster" ).asBoolean( false );

		JsonNode volume = node.path( "volume" );
		String volumeType = textOrNull( volume, "type" );

		JsonNode backupSchedule = node.path( "backup_schedule" );
		int backupRetentionDays = backupSchedule.path( "retention" ).asInt( 0 );

		JsonNode endpoint = node.path( "endpoint" );
		String endpointIp = textOrNull( endpoint, "ip" );
		boolean publiclyAccessible = endpoint.path( "publicly_accessible" ).asBoolean( false );

		List<String> tags = new ArrayList<>();
		JsonNode tagsNode = node.path( "tags" );
		if ( tagsNode.isArray() ) {
			for ( JsonNode tag : tagsNode ) {
				tags.add( tag.asText() );
			}
		}

		return new ScalewayDatabase(
				textOrNull( node, "id" ),
				textOrNull( node, "name" ),
				textOrNull( node, "status" ),
				textOrNull( node, "engine" ),
				volumeType,
				textOrNull( node, "region" ),
				textOrNull( node, "project_id" ),
				textOrNull( node, "organization_id" ),
				haEnabled,
				publiclyAccessible,
				backupRetentionDays,
				endpointIp,
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
