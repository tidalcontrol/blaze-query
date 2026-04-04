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
 * <p>Relevant compliance queries:
 * <ul>
 *   <li><b>Users with CRM contact access</b> – join with {@link HubspotOwner} on {@code email}</li>
 *   <li><b>Stale / inactive users</b> – filter by {@code status = 'INACTIVE'} or
 *       {@code updatedAt} older than a threshold</li>
 *   <li><b>Super-admin accounts</b> – filter by {@code superAdmin = true}</li>
 * </ul>
 *
 * <p>Note: HubSpot does not expose per-user MFA status via the Settings Users API.
 * To audit 2FA enforcement, query {@link HubspotAccountInfo} to verify the
 * portal's data-hosting location and account type, and enforce the policy at the
 * account level inside the HubSpot Security settings UI.
 *
 * @author Blazebit
 * @since 2.4.4
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public record HubspotUser(
		String id,
		String email,
		List<String> roleIds,
		String primaryTeamId,
		Boolean superAdmin,
		/** {@code ACTIVE} or {@code INACTIVE} */
		String status,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {
}
