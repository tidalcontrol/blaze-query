/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.hubspot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Represents a HubSpot CRM owner from the Owners API
 * ({@code GET /crm/v3/owners}).
 *
 * <p>Mirrors the {@code PublicOwner} schema. CRM owners are users (or queues)
 * that are assigned to CRM records (contacts, deals, etc.). This is the primary
 * way to identify which users have direct access to contact data.
 *
 * <p>Cross-reference {@link HubspotOwner#email()} with {@link HubspotUser#email()} to
 * enrich owner records with portal-user properties such as role assignments.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public record HubspotOwner(
		/** CRM owner ID — use this when assigning record ownership. */
		String id,
		/** Owner type: {@code PERSON} or {@code QUEUE}. */
		String type,
		/** Portal user ID — corresponds to {@link HubspotUser#id()} (cast to VARCHAR). */
		Integer userId,
		/** Portal user ID, including inactive users — populated when the underlying user has been deactivated. */
		Integer userIdIncludingInactive,
		String email,
		String firstName,
		String lastName,
		/** {@code true} if the owner has been archived / removed from the portal. */
		Boolean archived,
		/** Teams the owner belongs to (with a flag indicating their primary team). */
		List<OwnerTeam> teams,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {

	/**
	 * Team membership entry embedded in {@link HubspotOwner#teams()}. Note this is
	 * a different schema from {@link HubspotTeam} — it carries a {@code primary}
	 * flag indicating whether the team is the owner's primary team.
	 */
	@JsonIgnoreProperties( ignoreUnknown = true )
	public record OwnerTeam(
			String id,
			String name,
			Boolean primary
	) {
	}
}
