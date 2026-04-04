/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.hubspot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;

/**
 * Represents a HubSpot CRM owner from the Owners API
 * ({@code GET /crm/v3/owners}).
 *
 * <p>CRM owners are users that are assigned to CRM records (contacts, deals, etc.).
 * This is the primary way to identify which users have direct access to contact data.
 *
 * <p>Cross-reference {@link HubspotOwner#email()} with {@link HubspotUser#email()} to
 * enrich owner records with portal-user properties such as role assignments or status.
 *
 * @author Blazebit
 * @since 2.4.4
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public record HubspotOwner(
		/** CRM owner ID – use this when assigning record ownership. */
		String id,
		/** Portal user ID – corresponds to {@link HubspotUser#id()}. */
		Integer userId,
		String email,
		String firstName,
		String lastName,
		/** {@code true} if the owner has been archived / removed from the portal. */
		Boolean archived,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {
}
