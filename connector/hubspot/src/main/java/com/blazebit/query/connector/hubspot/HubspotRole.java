/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.hubspot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a HubSpot permission role from the Settings Roles API
 * ({@code GET /settings/v3/users/roles}).
 *
 * <p>Join with {@link HubspotUser#roleIds()} to determine which permissions each
 * user holds, supporting the "users that can access contact information" query.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public record HubspotRole(
		String id,
		String name,
		/** {@code true} if the role allows billing writes. */
		Boolean requiresBillingWrite
) {
}
