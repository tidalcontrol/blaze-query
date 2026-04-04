/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.hubspot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a single audit-log event from
 * {@code GET /account-info/v3/activity/audit-logs}.
 *
 * <p>Audit logs record who did what and when inside the portal, supporting
 * logging and monitoring compliance requirements.
 *
 * <p>Requires an Enterprise HubSpot subscription and the
 * {@code account-info.security.read} scope.
 *
 * @author Blazebit
 * @since 2.4.4
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public record HubspotAuditLog(
		String id,
		/**
		 * High-level event category, e.g. {@code CONTACT}, {@code DEAL},
		 * {@code USER}, {@code INTEGRATION}.
		 */
		String category,
		/**
		 * Fine-grained event sub-category, e.g. {@code CREATED}, {@code UPDATED},
		 * {@code DELETED}, {@code PUBLISHED}.
		 */
		String subCategory,
		/** Human-readable description of the action performed. */
		String action,
		/** ID of the object that was acted upon. */
		String targetObjectId,
		/** Type of the object that was acted upon. */
		String targetObjectType,
		/** ISO-8601 timestamp of the event. */
		String occurredAt,
		/** Portal user ID of the actor who performed the action. */
		@JsonProperty( "actingUser" ) HubspotAuditLog.ActingUser actingUser
) {

	/**
	 * Identifies the portal user who triggered the audit event.
	 */
	@JsonIgnoreProperties( ignoreUnknown = true )
	public record ActingUser(
			String userId,
			String userEmail
	) {
	}
}
