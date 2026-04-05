/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.hubspot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple HTTP client for the HubSpot REST API. Authenticate with a
 * <a href="https://developers.hubspot.com/docs/api/private-apps">Private App access token</a>.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public final class HubspotClient {

	private static final String BASE_URL = "https://api.hubapi.com";
	private static final int PAGE_SIZE = 100;

	private final String accessToken;
	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;

	/**
	 * Creates a new {@link HubspotClient} authenticated with the given Private App access token.
	 *
	 * @param accessToken HubSpot Private App access token
	 */
	public HubspotClient(String accessToken) {
		this.accessToken = accessToken;
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout( Duration.ofSeconds( 30 ) )
				.build();
		this.objectMapper = new ObjectMapper()
				.registerModule( new JavaTimeModule() )
				.disable( SerializationFeature.WRITE_DATES_AS_TIMESTAMPS );
	}

	// -------------------------------------------------------------------------
	// Settings – Users  (/settings/v3/users)
	// -------------------------------------------------------------------------

	/**
	 * Returns all portal users from the Settings Users API.
	 * Requires the {@code settings.users.read} scope.
	 */
	List<HubspotUser> listUsers() throws IOException, InterruptedException {
		List<HubspotUser> users = new ArrayList<>();
		String after = null;
		do {
			String url = BASE_URL + "/settings/v3/users?limit=" + PAGE_SIZE
					+ ( after != null ? "&after=" + after : "" );
			UserPageResponse page = get( url, UserPageResponse.class );
			if ( page.results != null ) {
				users.addAll( page.results );
			}
			after = ( page.paging != null && page.paging.next != null ) ? page.paging.next.after : null;
		} while ( after != null );
		return users;
	}

	// -------------------------------------------------------------------------
	// Settings – Roles  (/settings/v3/users/roles)
	// -------------------------------------------------------------------------

	/**
	 * Returns all roles defined on the portal.
	 * Requires the {@code settings.users.read} scope.
	 */
	List<HubspotRole> listRoles() throws IOException, InterruptedException {
		String url = BASE_URL + "/settings/v3/users/roles";
		RolePageResponse page = get( url, RolePageResponse.class );
		return page.results != null ? page.results : List.of();
	}

	// -------------------------------------------------------------------------
	// CRM Owners  (/crm/v3/owners)
	// -------------------------------------------------------------------------

	/**
	 * Returns all CRM owners (users that own CRM records such as contacts).
	 * Requires the {@code crm.objects.owners.read} scope.
	 */
	List<HubspotOwner> listOwners() throws IOException, InterruptedException {
		List<HubspotOwner> owners = new ArrayList<>();
		String after = null;
		do {
			String url = BASE_URL + "/crm/v3/owners?limit=" + PAGE_SIZE + "&archived=false"
					+ ( after != null ? "&after=" + after : "" );
			OwnerPageResponse page = get( url, OwnerPageResponse.class );
			if ( page.results != null ) {
				owners.addAll( page.results );
			}
			after = ( page.paging != null && page.paging.next != null ) ? page.paging.next.after : null;
		} while ( after != null );
		return owners;
	}

	// -------------------------------------------------------------------------
	// Teams  (/settings/v3/users/teams)
	// -------------------------------------------------------------------------

	/**
	 * Returns all teams defined on the portal.
	 * Available on all subscription tiers (read-only).
	 */
	List<HubspotTeam> listTeams() throws IOException, InterruptedException {
		TeamPageResponse page = get( BASE_URL + "/settings/v3/users/teams", TeamPageResponse.class );
		return page.results != null ? page.results : List.of();
	}

	// -------------------------------------------------------------------------
	// Communication Subscription Definitions  (/communication-preferences/v3/definitions)
	// -------------------------------------------------------------------------

	/**
	 * Returns all email consent subscription type definitions configured on the portal.
	 * Required to audit GDPR consent categories.
	 */
	List<HubspotSubscriptionDefinition> listSubscriptionDefinitions() throws IOException, InterruptedException {
		SubscriptionDefinitionPageResponse page = get(
				BASE_URL + "/communication-preferences/v3/definitions",
				SubscriptionDefinitionPageResponse.class );
		return page.subscriptionDefinitions != null ? page.subscriptionDefinitions : List.of();
	}

	// -------------------------------------------------------------------------
	// Account Info  (/account-info/v3/details)
	// -------------------------------------------------------------------------

	/**
	 * Returns portal-level account information including data hosting location
	 * (EU vs. US), which is relevant for GDPR compliance.
	 * Requires the {@code oauth} scope.
	 */
	HubspotAccountInfo getAccountInfo() throws IOException, InterruptedException {
		return get( BASE_URL + "/account-info/v3/details", HubspotAccountInfo.class );
	}

	// -------------------------------------------------------------------------
	// Audit Logs  (/account-info/v3/activity/audit-logs)
	// -------------------------------------------------------------------------

	/**
	 * Returns audit log events within the given time window (max 90-day lookback).
	 * Requires the {@code account-info.security.read} scope and an Enterprise subscription.
	 *
	 * @param from earliest event timestamp (ISO-8601), or {@code null} for no lower bound
	 * @param to   latest event timestamp (ISO-8601), or {@code null} for no upper bound
	 */
	List<HubspotAuditLog> listAuditLogs(OffsetDateTime from, OffsetDateTime to) throws IOException, InterruptedException {
		List<HubspotAuditLog> logs = new ArrayList<>();
		String after = null;
		do {
			StringBuilder url = new StringBuilder( BASE_URL )
					.append( "/account-info/v3/activity/audit-logs?limit=" ).append( PAGE_SIZE );
			if ( from != null ) {
				url.append( "&occurredAfter=" ).append( from );
			}
			if ( to != null ) {
				url.append( "&occurredBefore=" ).append( to );
			}
			if ( after != null ) {
				url.append( "&after=" ).append( after );
			}
			AuditLogPageResponse page = get( url.toString(), AuditLogPageResponse.class );
			if ( page.results != null ) {
				logs.addAll( page.results );
			}
			after = ( page.paging != null && page.paging.next != null ) ? page.paging.next.after : null;
		} while ( after != null );
		return logs;
	}

	// -------------------------------------------------------------------------
	// Login Activity  (/account-info/v3/activity/login)
	// -------------------------------------------------------------------------

	/**
	 * Returns login events within the given time window.
	 * Each event records whether MFA and/or SSO was used — the primary way to audit
	 * 2FA compliance per-user.
	 * Requires the {@code account-info.security.read} scope and an Enterprise subscription.
	 *
	 * @param from earliest event timestamp (ISO-8601), or {@code null} for no lower bound
	 * @param to   latest event timestamp (ISO-8601), or {@code null} for no upper bound
	 */
	List<HubspotLoginActivity> listLoginActivity(OffsetDateTime from, OffsetDateTime to)
			throws IOException, InterruptedException {
		List<HubspotLoginActivity> events = new ArrayList<>();
		String after = null;
		do {
			StringBuilder url = new StringBuilder( BASE_URL )
					.append( "/account-info/v3/activity/login?limit=" ).append( PAGE_SIZE );
			if ( from != null ) {
				url.append( "&occurredAfter=" ).append( from );
			}
			if ( to != null ) {
				url.append( "&occurredBefore=" ).append( to );
			}
			if ( after != null ) {
				url.append( "&after=" ).append( after );
			}
			LoginActivityPageResponse page = get( url.toString(), LoginActivityPageResponse.class );
			if ( page.results != null ) {
				events.addAll( page.results );
			}
			after = ( page.paging != null && page.paging.next != null ) ? page.paging.next.after : null;
		} while ( after != null );
		return events;
	}

	// -------------------------------------------------------------------------
	// Security Activity  (/account-info/v3/activity/security)
	// -------------------------------------------------------------------------

	/**
	 * Returns security configuration change events within the given time window.
	 * Tracks MFA/SSO toggling, user additions/removals, permission changes, and more.
	 * Requires the {@code account-info.security.read} scope and an Enterprise subscription.
	 *
	 * @param from earliest event timestamp (ISO-8601), or {@code null} for no lower bound
	 * @param to   latest event timestamp (ISO-8601), or {@code null} for no upper bound
	 */
	List<HubspotSecurityActivity> listSecurityActivity(OffsetDateTime from, OffsetDateTime to)
			throws IOException, InterruptedException {
		List<HubspotSecurityActivity> events = new ArrayList<>();
		String after = null;
		do {
			StringBuilder url = new StringBuilder( BASE_URL )
					.append( "/account-info/v3/activity/security?limit=" ).append( PAGE_SIZE );
			if ( from != null ) {
				url.append( "&occurredAfter=" ).append( from );
			}
			if ( to != null ) {
				url.append( "&occurredBefore=" ).append( to );
			}
			if ( after != null ) {
				url.append( "&after=" ).append( after );
			}
			SecurityActivityPageResponse page = get( url.toString(), SecurityActivityPageResponse.class );
			if ( page.results != null ) {
				events.addAll( page.results );
			}
			after = ( page.paging != null && page.paging.next != null ) ? page.paging.next.after : null;
		} while ( after != null );
		return events;
	}

	// -------------------------------------------------------------------------
	// Internal HTTP helper
	// -------------------------------------------------------------------------

	private <T> T get(String url, Class<T> responseType) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder()
				.uri( URI.create( url ) )
				.header( "Authorization", "Bearer " + accessToken )
				.header( "Content-Type", "application/json" )
				.GET()
				.build();
		HttpResponse<String> response = httpClient.send( request, HttpResponse.BodyHandlers.ofString() );
		int status = response.statusCode();
		if ( status < 200 || status >= 300 ) {
			throw new IOException( "HubSpot API returned HTTP " + status + ": " + response.body() );
		}
		return objectMapper.readValue( response.body(), responseType );
	}

	// -------------------------------------------------------------------------
	// Internal JSON response DTOs (package-private for testability)
	// -------------------------------------------------------------------------

	@JsonIgnoreProperties( ignoreUnknown = true )
	static class UserPageResponse {
		public List<HubspotUser> results;
		public Paging paging;
	}

	@JsonIgnoreProperties( ignoreUnknown = true )
	static class RolePageResponse {
		public List<HubspotRole> results;
	}

	@JsonIgnoreProperties( ignoreUnknown = true )
	static class OwnerPageResponse {
		public List<HubspotOwner> results;
		public Paging paging;
	}

	@JsonIgnoreProperties( ignoreUnknown = true )
	static class AuditLogPageResponse {
		public List<HubspotAuditLog> results;
		public Paging paging;
	}

	@JsonIgnoreProperties( ignoreUnknown = true )
	static class LoginActivityPageResponse {
		public List<HubspotLoginActivity> results;
		public Paging paging;
	}

	@JsonIgnoreProperties( ignoreUnknown = true )
	static class SecurityActivityPageResponse {
		public List<HubspotSecurityActivity> results;
		public Paging paging;
	}

	@JsonIgnoreProperties( ignoreUnknown = true )
	static class TeamPageResponse {
		public List<HubspotTeam> results;
	}

	@JsonIgnoreProperties( ignoreUnknown = true )
	static class SubscriptionDefinitionPageResponse {
		public List<HubspotSubscriptionDefinition> subscriptionDefinitions;
	}

	@JsonIgnoreProperties( ignoreUnknown = true )
	static class Paging {
		public PagingNext next;
	}

	@JsonIgnoreProperties( ignoreUnknown = true )
	static class PagingNext {
		public String after;
		public String link;
	}
}
