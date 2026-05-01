/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */

/**
 * HubSpot connector for blaze-query.
 *
 * <p>Provides security and compliance data fetchers for:
 * <ul>
 *   <li>Users with CRM contact access ({@link com.blazebit.query.connector.hubspot.HubspotUser},
 *       {@link com.blazebit.query.connector.hubspot.HubspotOwner})</li>
 *   <li>2FA / MFA enforcement ({@link com.blazebit.query.connector.hubspot.HubspotUser} superAdmin flag
 *       combined with account security settings)</li>
 *   <li>Stale users ({@link com.blazebit.query.connector.hubspot.HubspotUser} status and updatedAt)</li>
 *   <li>GDPR governance ({@link com.blazebit.query.connector.hubspot.HubspotAccountInfo} dataHostingLocation)</li>
 *   <li>Audit logging and monitoring ({@link com.blazebit.query.connector.hubspot.HubspotAuditLog})</li>
 * </ul>
 *
 * <p>Configure via {@link com.blazebit.query.connector.hubspot.HubspotConnectorConfig}.
 */
package com.blazebit.query.connector.hubspot;
