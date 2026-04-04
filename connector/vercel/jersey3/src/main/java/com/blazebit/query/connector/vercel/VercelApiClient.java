/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.vercel;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * HTTP client for the Vercel REST API.
 *
 * <p>Configure with a personal access token (and optionally a team ID for team-scoped queries).
 * Create via {@code new VercelApiClient(token)} or {@code new VercelApiClient(token, teamId)}.
 *
 * @author Blazebit
 * @since 1.0.0
 */
public class VercelApiClient {

	static final String BASE_URL = "https://api.vercel.com";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
			.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false )
			.registerModule( new JavaTimeModule() );

	private final String accessToken;
	private final String teamId;

	/**
	 * Creates a client for user-level operations.
	 *
	 * @param accessToken Vercel personal access token
	 */
	public VercelApiClient(String accessToken) {
		this( accessToken, null );
	}

	/**
	 * Creates a client scoped to a specific team.
	 *
	 * @param accessToken Vercel personal access token
	 * @param teamId      Vercel team ID (e.g. {@code team_xxxxxxxx})
	 */
	public VercelApiClient(String accessToken, String teamId) {
		if ( accessToken == null || accessToken.isBlank() ) {
			throw new IllegalArgumentException( "accessToken must not be null or blank" );
		}
		this.accessToken = accessToken;
		this.teamId = teamId;
	}

	/**
	 * Fetches all pages of a paginated list endpoint.
	 *
	 * @param path     API path (e.g. {@code /v6/user/tokens})
	 * @param itemsKey JSON key holding the items array (e.g. {@code "tokens"});
	 *                 use {@code null} when the response itself is a JSON array
	 * @param itemType class of each item
	 * @param <T>      item type
	 * @return combined list across all pages
	 * @throws IOException on HTTP or JSON errors
	 */
	public <T> List<T> fetchPagedList(String path, String itemsKey, Class<T> itemType) throws IOException {
		List<T> result = new ArrayList<>();
		try ( Client client = ClientBuilder.newClient() ) {
			Long until = null;
			do {
				WebTarget target = client.target( BASE_URL ).path( path ).queryParam( "limit", "100" );
				if ( teamId != null ) {
					target = target.queryParam( "teamId", teamId );
				}
				if ( until != null ) {
					target = target.queryParam( "until", until );
				}
				Response response = target.request()
						.header( "Authorization", "Bearer " + accessToken )
						.get();
				checkResponse( path, response );
				String json = response.readEntity( String.class );
				JsonNode root = OBJECT_MAPPER.readTree( json );

				if ( root.isArray() ) {
					// Some endpoints return a bare array (e.g. /v1/webhooks)
					for ( JsonNode node : root ) {
						result.add( OBJECT_MAPPER.treeToValue( node, itemType ) );
					}
					break;
				}

				JsonNode items = itemsKey != null ? root.get( itemsKey ) : root;
				if ( items != null && items.isArray() ) {
					for ( JsonNode node : items ) {
						result.add( OBJECT_MAPPER.treeToValue( node, itemType ) );
					}
				}

				JsonNode pagination = root.get( "pagination" );
				JsonNode nextNode = pagination != null ? pagination.get( "next" ) : null;
				until = ( nextNode != null && !nextNode.isNull() && nextNode.asLong() != 0 )
						? nextNode.asLong()
						: null;
			}
			while ( until != null );
		}
		return result;
	}

	/**
	 * Fetches a paginated list of team-member-like resources scoped by a path-level team ID.
	 * The {@code teamId} query parameter is NOT added; the team ID is embedded in {@code path}.
	 *
	 * @param path     full API path including team ID (e.g. {@code /v3/teams/team_xxx/members})
	 * @param itemsKey JSON key holding the items array
	 * @param itemType class of each item
	 * @param <T>      item type
	 * @return combined list across all pages
	 * @throws IOException on HTTP or JSON errors
	 */
	public <T> List<T> fetchPagedListByPath(String path, String itemsKey, Class<T> itemType)
			throws IOException {
		List<T> result = new ArrayList<>();
		try ( Client client = ClientBuilder.newClient() ) {
			Long until = null;
			do {
				WebTarget target = client.target( BASE_URL ).path( path ).queryParam( "limit", "100" );
				if ( until != null ) {
					target = target.queryParam( "until", until );
				}
				Response response = target.request()
						.header( "Authorization", "Bearer " + accessToken )
						.get();
				checkResponse( path, response );
				String json = response.readEntity( String.class );
				JsonNode root = OBJECT_MAPPER.readTree( json );

				JsonNode items = itemsKey != null ? root.get( itemsKey ) : root;
				if ( items != null && items.isArray() ) {
					for ( JsonNode node : items ) {
						result.add( OBJECT_MAPPER.treeToValue( node, itemType ) );
					}
				}

				JsonNode pagination = root.get( "pagination" );
				JsonNode nextNode = pagination != null ? pagination.get( "next" ) : null;
				until = ( nextNode != null && !nextNode.isNull() && nextNode.asLong() != 0 )
						? nextNode.asLong()
						: null;
			}
			while ( until != null );
		}
		return result;
	}

	/**
	 * Fetches a single JSON object from the given path.
	 *
	 * @param path     API path
	 * @param itemType response class
	 * @param <T>      response type
	 * @return deserialized response
	 * @throws IOException on HTTP or JSON errors
	 */
	public <T> T fetchSingle(String path, Class<T> itemType) throws IOException {
		try ( Client client = ClientBuilder.newClient() ) {
			WebTarget target = client.target( BASE_URL ).path( path );
			if ( teamId != null ) {
				target = target.queryParam( "teamId", teamId );
			}
			Response response = target.request()
					.header( "Authorization", "Bearer " + accessToken )
					.get();
			checkResponse( path, response );
			return OBJECT_MAPPER.readValue( response.readEntity( String.class ), itemType );
		}
	}

	/**
	 * Returns the team ID this client is scoped to, or {@code null} if user-level.
	 */
	public String getTeamId() {
		return teamId;
	}

	private void checkResponse(String path, Response response) {
		if ( response.getStatus() < 200 || response.getStatus() >= 300 ) {
			String body = response.readEntity( String.class );
			throw new RuntimeException(
					"Vercel API returned HTTP " + response.getStatus() + " for " + path + ": " + body );
		}
	}
}
