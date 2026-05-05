/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.hubspot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Represents portal-level account information from
 * {@code GET /account-info/v3/details}.
 *
 * <p>Mirrors the {@code PortalInformationResponse} schema.
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
		/** Primary ISO-4217 currency code for the company, e.g. {@code "USD"}. */
		String companyCurrency,
		/** Additional ISO-4217 currency codes configured for the portal. */
		List<String> additionalCurrencies,
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
		 * Portal account type: {@code "STANDARD"}, {@code "APP_DEVELOPER"},
		 * {@code "SANDBOX"}, or {@code "DEVELOPER_TEST"}.
		 */
		String accountType
) {
}
