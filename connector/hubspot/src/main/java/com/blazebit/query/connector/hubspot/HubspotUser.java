/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.hubspot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Represents a HubSpot portal user from the Settings Users API
 * ({@code GET /settings/v3/users}).
 *
 * <p>Mirrors the {@code PublicUser} schema. Note that {@code status},
 * {@code createdAt}, and {@code updatedAt} are not in the published OpenAPI spec
 * but have been observed on responses and are kept for backwards compatibility
 * with existing queries — they may be {@code null} for portals that do not
 * surface them.
 *
 * <p>Relevant compliance queries:
 * <ul>
 *   <li><b>Users with CRM contact access</b> – join with {@link HubspotOwner} on {@code email}</li>
 *   <li><b>Super-admin accounts</b> – filter by {@code superAdmin = true}</li>
 * </ul>
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public record HubspotUser(
		String id,
		String email,
		String firstName,
		String lastName,
		/** Single role ID assigned to the user (legacy single-role field). */
		String roleId,
		/** All role IDs assigned to the user. */
		List<String> roleIds,
		String primaryTeamId,
		List<String> secondaryTeamIds,
		Boolean superAdmin,
		/** {@code ACTIVE} or {@code INACTIVE}. Not in the OpenAPI spec; may be {@code null}. */
		String status,
		/** Not in the OpenAPI spec; may be {@code null}. */
		OffsetDateTime createdAt,
		/** Not in the OpenAPI spec; may be {@code null}. */
		OffsetDateTime updatedAt
) {
}
