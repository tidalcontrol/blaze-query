/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * HTTP client wrapper for the Scaleway REST API.
 * Handles authentication, pagination, and JSON deserialization for the
 * IAM, Instance, Secret Manager, Key Manager, Audit Trail, Kubernetes,
 * Container Registry, and VPC APIs.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public class ScalewayClient implements Serializable {

	private static final String BASE_URL = "https://api.scaleway.com";
	private static final String IAM_BASE_URL = BASE_URL + "/iam/v1alpha1";
	private static final String INSTANCE_BASE_URL = BASE_URL + "/instance/v1/zones";
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
	 * @param zones          list of zones to query for zone-based APIs (e.g. "fr-par-1", "nl-ams-1")
	 */
	public ScalewayClient(String secretKey, String organizationId, List<String> zones) {
		this.secretKey = secretKey;
		this.organizationId = organizationId;
		this.zones = zones != null ? zones : List.of( "fr-par-1", "fr-par-2", "nl-ams-1", "pl-waw-1" );
	}

	/**
	 * Derives the unique set of regions from the configured zones by stripping the
	 * trailing zone index (e.g. "fr-par-1" → "fr-par", "nl-ams-1" → "nl-ams").
	 */
	List<String> regions() {
		Set<String> regions = new LinkedHashSet<>();
		for ( String zone : zones ) {
			int lastDash = zone.lastIndexOf( '-' );
			regions.add( lastDash > 0 ? zone.substring( 0, lastDash ) : zone );
		}
		return new ArrayList<>( regions );
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

	/** Lists all IAM users in the organization with full pagination. */
	public List<JsonNode> listIamUsers() throws IOException, InterruptedException {
		return fetchIamPaged( "/users?order_by=created_at_asc&organization_id=" + organizationId, "users" );
	}

	/** Lists all IAM groups in the organization with full pagination. */
	public List<JsonNode> listIamGroups() throws IOException, InterruptedException {
		return fetchIamPaged( "/groups?order_by=created_at_asc&organization_id=" + organizationId, "groups" );
	}

	/** Lists all IAM applications in the organization with full pagination. */
	public List<JsonNode> listIamApplications() throws IOException, InterruptedException {
		return fetchIamPaged( "/applications?order_by=created_at_asc&organization_id=" + organizationId, "applications" );
	}

	/** Lists all IAM API keys in the organization with full pagination. */
	public List<JsonNode> listIamApiKeys() throws IOException, InterruptedException {
		return fetchIamPaged( "/api-keys?order_by=created_at_asc&organization_id=" + organizationId, "api_keys" );
	}

	/** Lists all IAM policies in the organization with full pagination. */
	public List<JsonNode> listIamPolicies() throws IOException, InterruptedException {
		return fetchIamPaged( "/policies?order_by=created_at_asc&organization_id=" + organizationId, "policies" );
	}

	/** Lists all SSH keys in the organization with full pagination. */
	public List<JsonNode> listIamSshKeys() throws IOException, InterruptedException {
		return fetchIamPaged( "/ssh-keys?order_by=created_at_asc&organization_id=" + organizationId, "ssh_keys" );
	}

	// -------------------------------------------------------------------------
	// Instance API (zone-based)
	// -------------------------------------------------------------------------

	/** Lists all instances/servers across all configured zones with full pagination. */
	public List<JsonNode> listInstances() throws IOException, InterruptedException {
		List<JsonNode> result = new ArrayList<>();
		for ( String zone : zones ) {
			result.addAll( fetchZonePaged( INSTANCE_BASE_URL, zone, "/servers", "servers" ) );
		}
		return result;
	}

	/** Lists all security groups across all configured zones with full pagination. */
	public List<JsonNode> listSecurityGroups() throws IOException, InterruptedException {
		List<JsonNode> result = new ArrayList<>();
		for ( String zone : zones ) {
			result.addAll( fetchZonePaged( INSTANCE_BASE_URL, zone, "/security_groups", "security_groups" ) );
		}
		return result;
	}

	/** Lists all rules for a specific security group in a given zone. */
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
	// Secret Manager API (region-based)
	// -------------------------------------------------------------------------

	/** Lists all secrets across all derived regions with full pagination. */
	public List<JsonNode> listSecrets() throws IOException, InterruptedException {
		List<JsonNode> result = new ArrayList<>();
		for ( String region : regions() ) {
			result.addAll( fetchRegionPaged( "/secret-manager/v1beta1/regions", region, "/secrets", "secrets" ) );
		}
		return result;
	}

	/** Lists all secret versions for a given secret ID and region. */
	public List<JsonNode> listSecretVersions(String secretId, String region)
			throws IOException, InterruptedException {
		return fetchRegionPaged( "/secret-manager/v1beta1/regions", region,
				"/secrets/" + secretId + "/versions", "versions" );
	}

	// -------------------------------------------------------------------------
	// Key Manager (KMS) API (region-based)
	// -------------------------------------------------------------------------

	/** Lists all KMS keys across all derived regions with full pagination. */
	public List<JsonNode> listKmsKeys() throws IOException, InterruptedException {
		List<JsonNode> result = new ArrayList<>();
		for ( String region : regions() ) {
			result.addAll( fetchRegionPaged( "/key-manager/v1alpha1/regions", region, "/keys", "keys" ) );
		}
		return result;
	}

	// -------------------------------------------------------------------------
	// Audit Trail API (region-based)
	// -------------------------------------------------------------------------

	/** Lists recent audit trail events across all derived regions with full pagination. */
	public List<JsonNode> listAuditEvents() throws IOException, InterruptedException {
		List<JsonNode> result = new ArrayList<>();
		for ( String region : regions() ) {
			result.addAll( fetchRegionPaged( "/audit-trail/v1alpha1/regions", region,
					"/events?organization_id=" + organizationId, "events" ) );
		}
		return result;
	}

	// -------------------------------------------------------------------------
	// Kubernetes Kapsule API (region-based)
	// -------------------------------------------------------------------------

	/** Lists all Kubernetes clusters across all derived regions with full pagination. */
	public List<JsonNode> listK8sClusters() throws IOException, InterruptedException {
		List<JsonNode> result = new ArrayList<>();
		for ( String region : regions() ) {
			result.addAll( fetchRegionPaged( "/k8s/v1/regions", region, "/clusters", "clusters" ) );
		}
		return result;
	}

	// -------------------------------------------------------------------------
	// Container Registry API (region-based)
	// -------------------------------------------------------------------------

	/** Lists all container registry namespaces across all derived regions. */
	public List<JsonNode> listRegistryNamespaces() throws IOException, InterruptedException {
		List<JsonNode> result = new ArrayList<>();
		for ( String region : regions() ) {
			result.addAll( fetchRegionPaged( "/registry/v1/regions", region, "/namespaces", "namespaces" ) );
		}
		return result;
	}

	/** Lists all container images across all derived regions. */
	public List<JsonNode> listRegistryImages() throws IOException, InterruptedException {
		List<JsonNode> result = new ArrayList<>();
		for ( String region : regions() ) {
			result.addAll( fetchRegionPaged( "/registry/v1/regions", region, "/images", "images" ) );
		}
		return result;
	}

	// -------------------------------------------------------------------------
	// VPC API (region-based)
	// -------------------------------------------------------------------------

	/** Lists all VPCs across all derived regions with full pagination. */
	public List<JsonNode> listVpcs() throws IOException, InterruptedException {
		List<JsonNode> result = new ArrayList<>();
		for ( String region : regions() ) {
			result.addAll( fetchRegionPaged( "/vpc/v2/regions", region, "/vpcs", "vpcs" ) );
		}
		return result;
	}

	/** Lists all private networks across all derived regions with full pagination. */
	public List<JsonNode> listPrivateNetworks() throws IOException, InterruptedException {
		List<JsonNode> result = new ArrayList<>();
		for ( String region : regions() ) {
			result.addAll( fetchRegionPaged( "/vpc/v2/regions", region, "/private-networks", "private_networks" ) );
		}
		return result;
	}

	// -------------------------------------------------------------------------
	// Object Storage API (region-based)
	// -------------------------------------------------------------------------

	/** Lists all Object Storage buckets across all derived regions with full pagination. */
	public List<JsonNode> listObjectStorageBuckets() throws IOException, InterruptedException {
		List<JsonNode> result = new ArrayList<>();
		for ( String region : regions() ) {
			result.addAll( fetchRegionPaged( "/object-storage/v1/regions", region, "/buckets", "buckets" ) );
		}
		return result;
	}

	// -------------------------------------------------------------------------
	// RDB (Managed Databases) API (region-based)
	// -------------------------------------------------------------------------

	/** Lists all managed database instances across all derived regions with full pagination. */
	public List<JsonNode> listDatabases() throws IOException, InterruptedException {
		List<JsonNode> result = new ArrayList<>();
		for ( String region : regions() ) {
			result.addAll( fetchRegionPaged( "/rdb/v1/regions", region, "/instances", "instances" ) );
		}
		return result;
	}

	// -------------------------------------------------------------------------
	// Serverless Containers API (region-based)
	// -------------------------------------------------------------------------

	/** Lists all serverless containers across all derived regions with full pagination. */
	public List<JsonNode> listContainers() throws IOException, InterruptedException {
		List<JsonNode> result = new ArrayList<>();
		for ( String region : regions() ) {
			result.addAll( fetchRegionPaged( "/containers/v1beta1/regions", region, "/containers", "containers" ) );
		}
		return result;
	}

	// -------------------------------------------------------------------------
	// Serverless Functions API (region-based)
	// -------------------------------------------------------------------------

	/** Lists all serverless functions across all derived regions with full pagination. */
	public List<JsonNode> listFunctions() throws IOException, InterruptedException {
		List<JsonNode> result = new ArrayList<>();
		for ( String region : regions() ) {
			result.addAll( fetchRegionPaged( "/functions/v1beta1/regions", region, "/functions", "functions" ) );
		}
		return result;
	}

	// -------------------------------------------------------------------------
	// Block Storage Volumes API (zone-based, dict response)
	// -------------------------------------------------------------------------

	/**
	 * Lists all block storage volumes across all configured zones.
	 * The API returns volumes as a dict keyed by volume ID; zone is injected as "_zone".
	 */
	public List<JsonNode> listVolumes() throws IOException, InterruptedException {
		List<JsonNode> result = new ArrayList<>();
		for ( String zone : zones ) {
			String url = INSTANCE_BASE_URL + "/" + zone + "/volumes?per_page=" + PAGE_SIZE;
			JsonNode response = get( url );
			JsonNode volumesNode = response.path( "volumes" );
			if ( volumesNode.isObject() ) {
				volumesNode.fields().forEachRemaining( entry -> {
					JsonNode volumeNode = entry.getValue();
					if ( volumeNode instanceof ObjectNode ) {
						( (ObjectNode) volumeNode ).put( "_zone", zone );
					}
					result.add( volumeNode );
				} );
			}
		}
		return result;
	}

	// -------------------------------------------------------------------------
	// Snapshots API (zone-based)
	// -------------------------------------------------------------------------

	/** Lists all snapshots across all configured zones; zone is injected as "_zone". */
	public List<JsonNode> listSnapshots() throws IOException, InterruptedException {
		List<JsonNode> result = new ArrayList<>();
		for ( String zone : zones ) {
			List<JsonNode> zoneSnapshots = fetchZonePaged( INSTANCE_BASE_URL, zone, "/snapshots", "snapshots" );
			for ( JsonNode node : zoneSnapshots ) {
				if ( node instanceof ObjectNode ) {
					( (ObjectNode) node ).put( "_zone", zone );
				}
				result.add( node );
			}
		}
		return result;
	}

	// -------------------------------------------------------------------------
	// Load Balancer API (zone-based)
	// -------------------------------------------------------------------------

	/** Lists all load balancers across all configured zones with full pagination. */
	public List<JsonNode> listLoadBalancers() throws IOException, InterruptedException {
		List<JsonNode> result = new ArrayList<>();
		for ( String zone : zones ) {
			result.addAll( fetchZonePaged( BASE_URL + "/lb/v1/zones", zone, "/lbs", "lbs" ) );
		}
		return result;
	}

	/** Lists all frontends for a given load balancer. */
	public List<JsonNode> listLoadBalancerFrontends(String lbId, String zone)
			throws IOException, InterruptedException {
		String url = BASE_URL + "/lb/v1/zones/" + zone + "/lbs/" + lbId + "/frontends";
		JsonNode response = get( url );
		List<JsonNode> result = new ArrayList<>();
		JsonNode frontends = response.path( "frontends" );
		if ( frontends.isArray() ) {
			for ( JsonNode f : frontends ) {
				result.add( f );
			}
		}
		return result;
	}

	// -------------------------------------------------------------------------
	// Flexible IP API (zone-based)
	// -------------------------------------------------------------------------

	/** Lists all flexible IPs across all configured zones with full pagination. */
	public List<JsonNode> listFlexibleIps() throws IOException, InterruptedException {
		List<JsonNode> result = new ArrayList<>();
		for ( String zone : zones ) {
			result.addAll( fetchZonePaged( BASE_URL + "/flexible-ip/v1alpha1/zones", zone, "/fips", "flexible_ips" ) );
		}
		return result;
	}

	// -------------------------------------------------------------------------
	// Cockpit API (region-based)
	// -------------------------------------------------------------------------

	/** Gets the Cockpit alert manager configuration for the given region. */
	public JsonNode getCockpitAlertManager(String region) throws IOException, InterruptedException {
		String url = BASE_URL + "/cockpit/v1/regions/" + region + "/alertmanager";
		return get( url );
	}

	/** Returns the number of contact points configured for the Cockpit in the given region. */
	public int getCockpitContactPointCount(String region) throws IOException, InterruptedException {
		String url = BASE_URL + "/cockpit/v1/regions/" + region + "/contact-points?page_size=1";
		JsonNode response = get( url );
		return response.path( "total_count" ).asInt( 0 );
	}

	// -------------------------------------------------------------------------
	// Transactional Email (TEM) API (region-based)
	// -------------------------------------------------------------------------

	/** Lists all transactional email domains across all derived regions with full pagination. */
	public List<JsonNode> listTemDomains() throws IOException, InterruptedException {
		List<JsonNode> result = new ArrayList<>();
		for ( String region : regions() ) {
			result.addAll( fetchRegionPaged( "/transactional-email/v1alpha1/regions", region, "/domains", "domains" ) );
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

	private List<JsonNode> fetchZonePaged(String baseUrl, String zone, String path, String arrayField)
			throws IOException, InterruptedException {
		List<JsonNode> result = new ArrayList<>();
		int page = 1;
		while ( true ) {
			String url = baseUrl + "/" + zone + path + "?per_page=" + PAGE_SIZE + "&page=" + page;
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

	private List<JsonNode> fetchRegionPaged(String serviceBase, String region, String path, String arrayField)
			throws IOException, InterruptedException {
		List<JsonNode> result = new ArrayList<>();
		int page = 1;
		while ( true ) {
			String separator = path.contains( "?" ) ? "&" : "?";
			String url = BASE_URL + serviceBase + "/" + region + path
					+ separator + "page_size=" + PAGE_SIZE + "&page=" + page;
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
			throw new IOException( "Scaleway API request failed [" + response.statusCode() + "]: "
					+ url + " — " + response.body() );
		}
		return objectMapper().readTree( response.body() );
	}
}
