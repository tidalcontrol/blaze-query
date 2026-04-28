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
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public record ScalewayCockpitAlertManager(
		String region,
		String projectId,
		boolean managedAlertsEnabled,
		int contactPointCount
) {

	public static ScalewayCockpitAlertManager from(JsonNode alertManagerNode, String region, String projectId, int contactPointCount) {
		boolean managedAlertsEnabled = alertManagerNode.path( "alert_manager_enabled" ).asBoolean( false );
		return new ScalewayCockpitAlertManager(
				region,
				projectId,
				managedAlertsEnabled,
				contactPointCount
		);
	}
}
