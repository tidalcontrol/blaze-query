/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.hubspot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents portal-level account information from
 * {@code GET /account-info/v3/details}.
 *
 * <p>Relevant compliance queries:
 * <ul>
 *   <li><b>GDPR governance</b> – {@code dataHostingLocation = 'eu1'} indicates data is
 *       stored in the EU. For full GDPR compliance, verify that privacy and consent
 *       settings are also configured inside the HubSpot portal settings UI.</li>
 *   <li><b>Account type</b> – distinguishes production portals from sandbox / developer
 *       test accounts ({@code accountType = 'STANDARD'}).</li>
 * </ul>
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public record HubspotAccountInfo(
		Long portalId,
		/** IANA time zone identifier, e.g. {@code "US/Eastern"}. */
		String timeZone,
		/** ISO-4217 currency code, e.g. {@code "USD"}. */
		String currency,
		String utcOffset,
		Long utcOffsetMilliseconds,
		/** Subdomain used for this account's HubSpot UI, e.g. {@code "app.hubspot.com"}. */
		String uiDomain,
		/**
		 * Data-hosting region for this portal.
		 * {@code "na1"} = North America, {@code "eu1"} = European Union.
		 * EU hosting is required for strict GDPR data-residency compliance.
		 */
		String dataHostingLocation,
		/**
		 * Portal account type: {@code "STANDARD"}, {@code "DEVELOPER_TEST"},
		 * {@code "SANDBOX"}, or {@code "LEGACY_DEVELOPER"}.
		 */
		String accountType
) {
}
