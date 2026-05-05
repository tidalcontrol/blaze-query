/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.hubspot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a security activity event from
 * {@code GET /account-info/v3/activity/security}.
 *
 * <p>Mirrors the {@code HydratedCriticalAction} schema. The HubSpot API does not
 * expose severity, affected-user, or free-form detail fields — only the
 * {@link #type()} enum value categorises the action.
 *
 * <p>Requires an Enterprise HubSpot subscription and the
 * {@code account-info.security.read} scope.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public record HubspotSecurityActivity(
		String id,
		/** The action type, e.g. {@code MFA_DISABLED}, {@code SSO_CONFIGURED}, {@code PERMISSION_CHANGED}. */
		String type,
		/** ISO-8601 timestamp at which the action occurred. */
		String createdAt,
		/** Portal user ID associated with the action. */
		Integer userId,
		/** Display name (or email) of the user who performed the action — plain string, not a structured object. */
		String actingUser,
		/** Source IP address that performed the action. */
		String ipAddress,
		/** Approximate human-readable location derived from the IP address. */
		String location,
		/** ISO-3166-1 alpha-2 country code derived from the IP address. */
		String countryCode,
		/** Region/state code derived from the IP address. */
		String regionCode,
		/** ID of the object the action targeted, when applicable. */
		String objectId,
		/** Link to additional information about the action. */
		String infoUrl
) {
}
