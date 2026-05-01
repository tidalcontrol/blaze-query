/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.hubspot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a security configuration change event from
 * {@code GET /account-info/v3/activity/security}.
 *
 * <p>Security activity events track changes to the portal's security posture:
 * toggling MFA or SSO, modifying user permissions, rotating API tokens,
 * and similar high-impact configuration operations.
 *
 * <p>Requires an Enterprise HubSpot subscription and the
 * {@code account-info.security.read} scope.
 *
 * <p>Example compliance queries:
 * <ul>
 *   <li>Find MFA toggle events:
 *       {@code WHERE eventType IN ('MFA_ENABLED', 'MFA_DISABLED')}</li>
 *   <li>Find SSO configuration changes:
 *       {@code WHERE eventType IN ('SSO_CONFIGURED', 'SSO_CHANGED')}</li>
 *   <li>Find high-severity events:
 *       {@code WHERE severity = 'HIGH' OR severity = 'CRITICAL'}</li>
 *   <li>Find permission changes:
 *       {@code WHERE eventType = 'PERMISSION_CHANGED'}</li>
 * </ul>
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public record HubspotSecurityActivity(
		String id,
		/**
		 * Type of security event, e.g.:
		 * {@code MFA_ENABLED}, {@code MFA_DISABLED},
		 * {@code SSO_CONFIGURED}, {@code SSO_CHANGED},
		 * {@code PASSWORD_RESET},
		 * {@code API_TOKEN_CREATED}, {@code API_TOKEN_REVOKED},
		 * {@code USER_ADDED}, {@code USER_REMOVED},
		 * {@code PERMISSION_CHANGED}, {@code IP_WHITELIST_MODIFIED}.
		 */
		String eventType,
		/** ISO-8601 timestamp of the security event. */
		String occurredAt,
		/** User ID of the actor who triggered the event. */
		String actingUserId,
		/** Email of the actor who triggered the event. */
		String actingUserEmail,
		/** User ID of the account that was affected (if applicable). */
		String affectedUserId,
		/**
		 * Severity rating of the event: {@code LOW}, {@code MEDIUM},
		 * {@code HIGH}, or {@code CRITICAL}.
		 */
		String severity,
		/** Additional context detail provided by HubSpot. */
		String details
) {
}
