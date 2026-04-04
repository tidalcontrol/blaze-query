/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents the Scaleway Cockpit Alert Manager configuration for a region.
 * Used for compliance checks such as: missing alert contacts,
 * disabled managed alerts, and observability coverage gaps.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public record ScalewayCockpitAlertManager(
		String region,
		String projectId,
		boolean managedAlertsEnabled,
		int contactPointCount
) {

	public static ScalewayCockpitAlertManager from(JsonNode alertManagerNode, String region, String projectId, int contactPointCount) {
		boolean managedAlertsEnabled = alertManagerNode.path( "managed_alerts_enabled" ).asBoolean( false );

		String resolvedProjectId = projectId;
		if ( resolvedProjectId == null || resolvedProjectId.isEmpty() ) {
			JsonNode projNode = alertManagerNode.path( "project_id" );
			if ( !projNode.isNull() && !projNode.isMissingNode() ) {
				resolvedProjectId = projNode.asText();
			}
			else {
				resolvedProjectId = "";
			}
		}

		return new ScalewayCockpitAlertManager(
				region,
				resolvedProjectId,
				managedAlertsEnabled,
				contactPointCount
		);
	}
}
