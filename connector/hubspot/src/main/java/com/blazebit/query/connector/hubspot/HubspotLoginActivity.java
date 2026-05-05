/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.hubspot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a single login event from
 * {@code GET /account-info/v3/activity/login}.
 *
 * <p>Fields mirror the {@code PublicLoginAudit} schema returned by the HubSpot
 * Account Information API. Use {@link #loginSucceeded()} to distinguish
 * successful from failed logins.
 *
 * <p>Requires an Enterprise HubSpot subscription and the
 * {@code account-info.security.read} scope.
 *
 * <p>Example compliance queries:
 * <ul>
 *   <li>Find failed login attempts:
 *       {@code WHERE loginSucceeded = false}</li>
 *   <li>Find logins from unexpected countries:
 *       {@code WHERE countryCode NOT IN ('US', 'DE', ...)}</li>
 * </ul>
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public record HubspotLoginActivity(
		String id,
		/** ISO-8601 timestamp of the login attempt. */
		String loginAt,
		/** {@code true} if the login attempt succeeded, {@code false} otherwise. */
		Boolean loginSucceeded,
		/** Portal user ID of the user who attempted to log in. */
		Integer userId,
		/** Email address of the user who attempted to log in. */
		String email,
		/** Source IP address of the login attempt. */
		String ipAddress,
		/** User-Agent string sent by the client. */
		String userAgent,
		/** Approximate human-readable location derived from the IP address. */
		String location,
		/** ISO-3166-1 alpha-2 country code derived from the IP address. */
		String countryCode,
		/** Region/state code derived from the IP address. */
		String regionCode
) {
}
