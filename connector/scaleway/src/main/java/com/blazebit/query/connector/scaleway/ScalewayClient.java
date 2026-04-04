/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * HTTP client wrapper for the Scaleway REST API.
 * Handles authentication, pagination, and JSON deserialization.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public class ScalewayClient implements Serializable {

	private static final String IAM_BASE_URL = "https://api.scaleway.com/iam/v1alpha1";
	private static final String INSTANCE_BASE_URL = "https://api.scaleway.com/instance/v1/zones";
	private static final int PAGE_SIZE = 100;

	private final String secretKey;
	private final String organizationId;
	private final List<String> zones;

	private transient HttpClient httpClient;
	private transient ObjectMapper objectMapper;

	/**
	 * Creates a Scaleway API client.
	 *
	 * @param secretKey      the API key secret used as X-Auth-Token
	 * @param organizationId the Scaleway organization ID
	 * @param zones          list of zones to query for Instance API (e.g. "fr-par-1", "nl-ams-1")
	 */
	public ScalewayClient(String secretKey, String organizationId, List<String> zones) {
		this.secretKey = secretKey;
		this.organizationId = organizationId;
		this.zones = zones != null ? zones : List.of( "fr-par-1", "fr-par-2", "nl-ams-1", "pl-waw-1" );
	}

	private HttpClient httpClient() {
		if ( httpClient == null ) {
			httpClient = HttpClient.newHttpClient();
		}
		return httpClient;
	}

	private ObjectMapper objectMapper() {
		if ( objectMapper == null ) {
			objectMapper = new ObjectMapper().registerModule( new JavaTimeModule() );
		}
		return objectMapper;
	}

	// -------------------------------------------------------------------------
	// IAM API
	// -------------------------------------------------------------------------

	/**
	 * Lists all IAM users in the organization with full pagination.
	 */
	public List<JsonNode> listIamUsers() throws IOException, InterruptedException {
		return fetchIamPaged( "/users?order_by=created_at_asc&organization_id=" + organizationId, "users" );
	}

	/**
	 * Lists all IAM groups in the organization with full pagination.
	 */
	public List<JsonNode> listIamGroups() throws IOException, InterruptedException {
		return fetchIamPaged( "/groups?order_by=created_at_asc&organization_id=" + organizationId, "groups" );
	}

	/**
	 * Lists all IAM applications in the organization with full pagination.
	 */
	public List<JsonNode> listIamApplications() throws IOException, InterruptedException {
		return fetchIamPaged( "/applications?order_by=created_at_asc&organization_id=" + organizationId, "applications" );
	}

	/**
	 * Lists all IAM API keys in the organization with full pagination.
	 */
	public List<JsonNode> listIamApiKeys() throws IOException, InterruptedException {
		return fetchIamPaged( "/api-keys?order_by=created_at_asc&organization_id=" + organizationId, "api_keys" );
	}

	/**
	 * Lists all IAM policies in the organization with full pagination.
	 */
	public List<JsonNode> listIamPolicies() throws IOException, InterruptedException {
		return fetchIamPaged( "/policies?order_by=created_at_asc&organization_id=" + organizationId, "policies" );
	}

	/**
	 * Lists all SSH keys in the organization with full pagination.
	 */
	public List<JsonNode> listIamSshKeys() throws IOException, InterruptedException {
		return fetchIamPaged( "/ssh-keys?order_by=created_at_asc&organization_id=" + organizationId, "ssh_keys" );
	}

	// -------------------------------------------------------------------------
	// Instance API
	// -------------------------------------------------------------------------

	/**
	 * Lists all instances/servers across all configured zones with full pagination.
	 */
	public List<JsonNode> listInstances() throws IOException, InterruptedException {
		List<JsonNode> result = new ArrayList<>();
		for ( String zone : zones ) {
			result.addAll( fetchInstancePaged( zone, "/servers", "servers" ) );
		}
		return result;
	}

	/**
	 * Lists all security groups across all configured zones with full pagination.
	 */
	public List<JsonNode> listSecurityGroups() throws IOException, InterruptedException {
		List<JsonNode> result = new ArrayList<>();
		for ( String zone : zones ) {
			result.addAll( fetchInstancePaged( zone, "/security_groups", "security_groups" ) );
		}
		return result;
	}

	/**
	 * Lists all rules for a specific security group in a given zone.
	 * Rules are returned as raw {@link JsonNode} objects for mapping in the data fetcher.
	 */
	public List<JsonNode> listSecurityGroupRuleNodes(String securityGroupId, String zone)
			throws IOException, InterruptedException {
		String url = INSTANCE_BASE_URL + "/" + zone + "/security_groups/" + securityGroupId + "/rules";
		JsonNode response = get( url );
		List<JsonNode> result = new ArrayList<>();
		JsonNode rules = response.path( "rules" );
		if ( rules.isArray() ) {
			for ( JsonNode rule : rules ) {
				result.add( rule );
			}
		}
		return result;
	}

	// -------------------------------------------------------------------------
	// Internal helpers
	// -------------------------------------------------------------------------

	private List<JsonNode> fetchIamPaged(String path, String arrayField)
			throws IOException, InterruptedException {
		List<JsonNode> result = new ArrayList<>();
		int page = 1;
		while ( true ) {
			String separator = path.contains( "?" ) ? "&" : "?";
			String url = IAM_BASE_URL + path + separator + "page_size=" + PAGE_SIZE + "&page=" + page;
			JsonNode response = get( url );
			JsonNode items = response.path( arrayField );
			if ( !items.isArray() || items.isEmpty() ) {
				break;
			}
			for ( JsonNode item : items ) {
				result.add( item );
			}
			if ( items.size() < PAGE_SIZE ) {
				break;
			}
			page++;
		}
		return result;
	}

	private List<JsonNode> fetchInstancePaged(String zone, String path, String arrayField)
			throws IOException, InterruptedException {
		List<JsonNode> result = new ArrayList<>();
		int page = 1;
		while ( true ) {
			String url = INSTANCE_BASE_URL + "/" + zone + path + "?per_page=" + PAGE_SIZE + "&page=" + page;
			JsonNode response = get( url );
			JsonNode items = response.path( arrayField );
			if ( !items.isArray() || items.isEmpty() ) {
				break;
			}
			for ( JsonNode item : items ) {
				result.add( item );
			}
			if ( items.size() < PAGE_SIZE ) {
				break;
			}
			page++;
		}
		return result;
	}

	private JsonNode get(String url) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder()
				.uri( URI.create( url ) )
				.header( "X-Auth-Token", secretKey )
				.header( "Accept", "application/json" )
				.GET()
				.build();
		HttpResponse<String> response = httpClient().send( request, HttpResponse.BodyHandlers.ofString() );
		if ( response.statusCode() < 200 || response.statusCode() >= 300 ) {
			throw new IOException( "Scaleway API request failed [" + response.statusCode() + "]: " + url + " — " + response.body() );
		}
		return objectMapper().readTree( response.body() );
	}
}
