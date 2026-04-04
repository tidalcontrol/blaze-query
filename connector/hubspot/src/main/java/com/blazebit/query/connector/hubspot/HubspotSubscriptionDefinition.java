/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.hubspot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a portal-level communication subscription type from
 * {@code GET /communication-preferences/v3/definitions}.
 *
 * <p>Subscription definitions are the consent categories a portal uses to obtain
 * and record GDPR-compliant email consent from contacts (e.g. "Marketing Emails",
 * "Product Updates", "Transactional").
 *
 * <p>This is the portal-level catalogue; combine with per-contact subscription
 * status to fully audit consent coverage.
 *
 * <p>Relevant compliance queries:
 * <ul>
 *   <li><b>GDPR consent categories exist</b> — verify at least one active subscription
 *       type is configured ({@code isActive = true})</li>
 *   <li><b>No default opt-in</b> — flag subscription types where {@code isDefault = true}
 *       as they silently subscribe contacts without explicit consent</li>
 *   <li><b>Internal-only types</b> — {@code isInternal = true} types are not shown to
 *       contacts; verify their use is intentional</li>
 * </ul>
 *
 * @author Blazebit
 * @since 2.4.4
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public record HubspotSubscriptionDefinition(
		String id,
		String name,
		String description,
		/** {@code true} if this subscription type is currently in use. Maps to the {@code isActive} JSON field. */
		@JsonProperty( "isActive" ) Boolean active,
		/**
		 * {@code true} if contacts are automatically opted in to this type.
		 * A value of {@code true} may indicate a GDPR risk (implicit consent).
		 * Maps to the {@code isDefault} JSON field.
		 */
		@JsonProperty( "isDefault" ) Boolean defaultOptIn,
		/**
		 * {@code true} if this subscription type is hidden from contact preference pages.
		 * Internal types should be reviewed to confirm they comply with consent requirements.
		 * Maps to the {@code isInternal} JSON field.
		 */
		@JsonProperty( "isInternal" ) Boolean internal,
		/** Communication channel: {@code EMAIL}, {@code WHATSAPP}, etc. */
		String communicationMethod,
		/** Intended purpose of this subscription (e.g. marketing, transactional). */
		String purpose,
		String createdAt,
		String updatedAt,
		@JsonProperty( "businessUnitId" ) Long businessUnitId
) {
}
