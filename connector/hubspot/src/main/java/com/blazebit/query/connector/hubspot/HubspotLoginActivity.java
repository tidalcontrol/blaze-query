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
 * <p>Each record captures the authentication method used for a login attempt,
 * including whether MFA and/or SSO were used. This is the primary data source
 * for 2FA compliance auditing since HubSpot does not expose a per-user
 * "MFA enabled" flag in the Users API.
 *
 * <p>Requires an Enterprise HubSpot subscription and the
 * {@code account-info.security.read} scope.
 *
 * <p>Example compliance queries:
 * <ul>
 *   <li>Find logins where MFA was not used:
 *       {@code WHERE mfaUsed = false AND loginStatus = 'SUCCESS'}</li>
 *   <li>Find logins that bypassed SSO:
 *       {@code WHERE ssoUsed = false AND loginMethod = 'PASSWORD'}</li>
 *   <li>Find failed login attempts:
 *       {@code WHERE loginStatus = 'FAILURE'}</li>
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
		/** Portal user ID of the user who attempted to log in. */
		String userId,
		/** Email address of the user who attempted to log in. */
		String userEmail,
		/** ISO-8601 timestamp of the login attempt. */
		String occurredAt,
		/**
		 * Channel through which login was attempted:
		 * {@code WEB}, {@code MOBILE_APP}, {@code API}.
		 */
		String loginType,
		/**
		 * Authentication method used:
		 * {@code PASSWORD}, {@code SSO}, {@code TWO_FACTOR}, {@code OAUTH}.
		 */
		String loginMethod,
		/**
		 * Outcome of the login attempt: {@code SUCCESS} or {@code FAILURE}.
		 */
		String loginStatus,
		/**
		 * {@code true} if Single Sign-On was used for this login.
		 * When {@code false} on a portal that should enforce SSO, this indicates
		 * a policy violation.
		 */
		Boolean ssoUsed,
		/**
		 * {@code true} if Multi-Factor Authentication (2FA) was completed for
		 * this login. Use {@code WHERE mfaUsed = false AND loginStatus = 'SUCCESS'}
		 * to identify logins that succeeded without 2FA.
		 */
		Boolean mfaUsed,
		/** Source IP address of the login attempt. */
		String ipAddress,
		/** ISO-3166-1 alpha-2 country code derived from the IP address. */
		String countryCode,
		/** Region/state code derived from the IP address. */
		String regionCode,
		/** Browser name and version string. */
		String browser,
		/** Device type (e.g. {@code DESKTOP}, {@code MOBILE}). */
		String device
) {
}
