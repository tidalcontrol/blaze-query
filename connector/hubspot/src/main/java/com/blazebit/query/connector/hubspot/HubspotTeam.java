/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.hubspot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Represents a HubSpot team from the Teams API
 * ({@code GET /settings/v3/users/teams}).
 *
 * <p>Teams scope visibility of CRM records and pipelines. Querying teams
 * alongside users reveals the organisational access structure:
 * <ul>
 *   <li><b>Separation of duties</b> — teams with a single member have no oversight</li>
 *   <li><b>Orphaned accounts</b> — users not assigned to any team</li>
 *   <li><b>Record access scope</b> — which team owns records in a pipeline</li>
 * </ul>
 *
 * <p>Available on all HubSpot subscription tiers (read-only).
 * Nested teams require an Enterprise subscription.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public record HubspotTeam(
		String id,
		String name,
		/** Primary members assigned directly to this team. */
		List<String> userIds,
		/** Secondary (additional) members assigned to this team. */
		List<String> secondaryUserIds,
		/** Parent team ID for nested team hierarchies (Enterprise only). */
		String parentTeamId
) {
}
