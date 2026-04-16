/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.notion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * HTTP client for the Notion REST API (v1).
 *
 * <p>Callers create one instance per integration token and pass it to the query context
 * via {@link NotionConnectorConfig#NOTION_CLIENT}. The client handles cursor-based
 * pagination transparently for all list and search operations.
 *
 * <p><b>Read operations</b> (used by DataFetchers):
 * <ul>
 *   <li>{@link #getMe()} — workspace info for the integration bot</li>
 *   <li>{@link #listUsers()} — all workspace users</li>
 *   <li>{@link #searchPages()} — all pages accessible to the integration</li>
 *   <li>{@link #searchDatabases()} — all databases accessible to the integration</li>
 *   <li>{@link #getBlockChildren(String)} — first-level blocks of a page or block</li>
 * </ul>
 *
 * <p><b>Write operations</b>:
 * <ul>
 *   <li>{@link #createPage(JsonNode)} — create a new page</li>
 *   <li>{@link #updatePage(String, JsonNode)} — update page properties or lifecycle state</li>
 *   <li>{@link #appendBlocks(String, List)} — append block content to a page or block</li>
 * </ul>
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class NotionClient {

	private static final String BASE_URL = "https://api.notion.com/v1";
	private static final String NOTION_VERSION = "2022-06-28";
	private static final int PAGE_SIZE = 100;

	private final String apiToken;
	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;

	/**
	 * Creates a new {@link NotionClient} for the given integration token.
	 *
	 * @param apiToken a valid Notion internal integration token (starts with {@code secret_})
	 */
	public NotionClient(String apiToken) {
		this.apiToken = apiToken;
		this.httpClient = HttpClient.newHttpClient();
		this.objectMapper = new ObjectMapper();
	}

	// -------------------------------------------------------------------------
	// Read operations
	// -------------------------------------------------------------------------

	/**
	 * Returns the bot user object for this integration, including workspace metadata.
	 *
	 * @return raw JSON node from {@code GET /v1/users/me}
	 * @throws IOException if an I/O error occurs
	 * @throws InterruptedException if the request is interrupted
	 */
	public JsonNode getMe() throws IOException, InterruptedException {
		HttpRequest request = newGetRequest( "/users/me" );
		return executeRequest( request );
	}

	/**
	 * Returns all users visible to the integration (both persons and bots).
	 *
	 * @return raw JSON nodes from {@code GET /v1/users}
	 * @throws IOException if an I/O error occurs
	 * @throws InterruptedException if the request is interrupted
	 */
	public List<JsonNode> listUsers() throws IOException, InterruptedException {
		return paginatedGet( "/users" );
	}

	/**
	 * Returns all pages visible to the integration.
	 *
	 * @return raw JSON nodes from {@code POST /v1/search} filtered to {@code object=page}
	 * @throws IOException if an I/O error occurs
	 * @throws InterruptedException if the request is interrupted
	 */
	public List<JsonNode> searchPages() throws IOException, InterruptedException {
		return paginatedSearch( "page" );
	}

	/**
	 * Returns all databases visible to the integration.
	 *
	 * @return raw JSON nodes from {@code POST /v1/search} filtered to {@code object=database}
	 * @throws IOException if an I/O error occurs
	 * @throws InterruptedException if the request is interrupted
	 */
	public List<JsonNode> searchDatabases() throws IOException, InterruptedException {
		return paginatedSearch( "database" );
	}

	/**
	 * Returns the first-level child blocks of a page or block.
	 *
	 * <p>Only direct children are returned. To read nested content, call this method
	 * recursively for any block where {@code has_children = true}.
	 *
	 * @param blockId the UUID of the page or block whose children to retrieve
	 * @return raw JSON nodes from {@code GET /v1/blocks/{blockId}/children}
	 * @throws IOException if an I/O error occurs
	 * @throws InterruptedException if the request is interrupted
	 */
	public List<JsonNode> getBlockChildren(String blockId) throws IOException, InterruptedException {
		return paginatedGet( "/blocks/" + blockId + "/children" );
	}

	/**
	 * Returns all comments on a page (top-level and block-anchored).
	 *
	 * <p>The Notion API accepts a {@code block_id} which can be a page UUID; it returns
	 * all comments associated with that page.
	 *
	 * @param pageId the UUID of the page whose comments to retrieve
	 * @return raw JSON nodes from {@code GET /v1/comments?block_id={pageId}}
	 * @throws IOException if an I/O error occurs
	 * @throws InterruptedException if the request is interrupted
	 */
	public List<JsonNode> listComments(String pageId) throws IOException, InterruptedException {
		return paginatedGet( "/comments?block_id=" + pageId );
	}

	/**
	 * Returns all rows in a database.
	 *
	 * @param databaseId the UUID of the database to query
	 * @return raw JSON nodes from {@code POST /v1/databases/{databaseId}/query}
	 * @throws IOException if an I/O error occurs
	 * @throws InterruptedException if the request is interrupted
	 */
	public List<JsonNode> queryDatabase(String databaseId) throws IOException, InterruptedException {
		List<JsonNode> results = new ArrayList<>();
		String cursor = null;
		do {
			ObjectNode requestBody = objectMapper.createObjectNode();
			requestBody.put( "page_size", PAGE_SIZE );
			if ( cursor != null ) {
				requestBody.put( "start_cursor", cursor );
			}
			JsonNode body = executeRequest( newPostRequest( "/databases/" + databaseId + "/query", requestBody ) );
			body.get( "results" ).forEach( results::add );
			cursor = nextCursor( body );
		}
		while ( cursor != null );
		return results;
	}

	// -------------------------------------------------------------------------
	// Write operations
	// -------------------------------------------------------------------------

	/**
	 * Creates a new page.
	 *
	 * <p>The {@code requestBody} must include a {@code parent} field specifying where the
	 * page should be created. Example minimal body:
	 * <pre>{@code
	 * {
	 *   "parent": { "page_id": "<parent-page-uuid>" },
	 *   "properties": {
	 *     "title": [{ "text": { "content": "My new page" } }]
	 *   }
	 * }
	 * }</pre>
	 *
	 * @param requestBody full {@code POST /v1/pages} request body as a Jackson node
	 * @return the created page as a JSON node
	 * @throws IOException if an I/O error occurs or the API returns an error status
	 * @throws InterruptedException if the request is interrupted
	 */
	public JsonNode createPage(JsonNode requestBody) throws IOException, InterruptedException {
		HttpRequest request = newPostRequest( "/pages", requestBody );
		return executeRequest( request );
	}

	/**
	 * Updates properties or lifecycle state of an existing page.
	 *
	 * <p>Supported fields include {@code properties}, {@code icon}, {@code cover},
	 * {@code is_locked}, {@code in_trash}, and {@code is_archived}. Example to trash a page:
	 * <pre>{@code
	 * ObjectNode body = objectMapper.createObjectNode();
	 * body.put("in_trash", true);
	 * client.updatePage(pageId, body);
	 * }</pre>
	 *
	 * @param pageId the UUID of the page to update
	 * @param requestBody fields to update as a Jackson node
	 * @return the updated page as a JSON node
	 * @throws IOException if an I/O error occurs or the API returns an error status
	 * @throws InterruptedException if the request is interrupted
	 */
	public JsonNode updatePage(String pageId, JsonNode requestBody) throws IOException, InterruptedException {
		HttpRequest request = newPatchRequest( "/pages/" + pageId, requestBody );
		return executeRequest( request );
	}

	/**
	 * Appends block children to a page or an existing block.
	 *
	 * <p>Each element of {@code children} must be a valid Notion block object. A maximum
	 * of 100 blocks may be appended per call. Example paragraph block:
	 * <pre>{@code
	 * ObjectNode para = objectMapper.createObjectNode();
	 * para.put("object", "block");
	 * para.put("type", "paragraph");
	 * ObjectNode text = para.putObject("paragraph").putArray("rich_text")
	 *     .addObject().putObject("text");
	 * text.put("content", "Hello, world!");
	 * client.appendBlocks(pageId, List.of(para));
	 * }</pre>
	 *
	 * @param blockId the UUID of the page or block to append children to
	 * @param children block objects to append (max 100)
	 * @return the list of newly created blocks as a JSON node
	 * @throws IOException if an I/O error occurs or the API returns an error status
	 * @throws InterruptedException if the request is interrupted
	 */
	public JsonNode appendBlocks(String blockId, List<JsonNode> children) throws IOException, InterruptedException {
		ObjectNode requestBody = objectMapper.createObjectNode();
		ArrayNode childArray = requestBody.putArray( "children" );
		children.forEach( childArray::add );
		HttpRequest request = newPatchRequest( "/blocks/" + blockId + "/children", requestBody );
		return executeRequest( request );
	}

	// -------------------------------------------------------------------------
	// Internals
	// -------------------------------------------------------------------------

	private List<JsonNode> paginatedGet(String path) throws IOException, InterruptedException {
		List<JsonNode> results = new ArrayList<>();
		String cursor = null;
		do {
			char separator = path.contains( "?" ) ? '&' : '?';
			String url = BASE_URL + path + separator + "page_size=" + PAGE_SIZE
					+ ( cursor != null ? "&start_cursor=" + cursor : "" );
			JsonNode body = executeRequest( newGetRequest( url, true ) );
			body.get( "results" ).forEach( results::add );
			cursor = nextCursor( body );
		}
		while ( cursor != null );
		return results;
	}

	private List<JsonNode> paginatedSearch(String objectType) throws IOException, InterruptedException {
		List<JsonNode> results = new ArrayList<>();
		String cursor = null;
		do {
			ObjectNode requestBody = objectMapper.createObjectNode();
			ObjectNode filter = requestBody.putObject( "filter" );
			filter.put( "value", objectType );
			filter.put( "property", "object" );
			requestBody.put( "page_size", PAGE_SIZE );
			if ( cursor != null ) {
				requestBody.put( "start_cursor", cursor );
			}
			JsonNode body = executeRequest( newPostRequest( "/search", requestBody ) );
			body.get( "results" ).forEach( results::add );
			cursor = nextCursor( body );
		}
		while ( cursor != null );
		return results;
	}

	private HttpRequest newGetRequest(String path) {
		return newGetRequest( path, false );
	}

	private HttpRequest newGetRequest(String path, boolean absoluteUrl) {
		String url = absoluteUrl ? path : BASE_URL + path;
		return HttpRequest.newBuilder()
				.uri( URI.create( url ) )
				.header( "Authorization", "Bearer " + apiToken )
				.header( "Notion-Version", NOTION_VERSION )
				.header( "Accept", "application/json" )
				.GET()
				.build();
	}

	private HttpRequest newPostRequest(String path, JsonNode body) throws IOException {
		return HttpRequest.newBuilder()
				.uri( URI.create( BASE_URL + path ) )
				.header( "Authorization", "Bearer " + apiToken )
				.header( "Notion-Version", NOTION_VERSION )
				.header( "Content-Type", "application/json" )
				.header( "Accept", "application/json" )
				.POST( HttpRequest.BodyPublishers.ofString( objectMapper.writeValueAsString( body ) ) )
				.build();
	}

	private HttpRequest newPatchRequest(String path, JsonNode body) throws IOException {
		return HttpRequest.newBuilder()
				.uri( URI.create( BASE_URL + path ) )
				.header( "Authorization", "Bearer " + apiToken )
				.header( "Notion-Version", NOTION_VERSION )
				.header( "Content-Type", "application/json" )
				.header( "Accept", "application/json" )
				.method( "PATCH", HttpRequest.BodyPublishers.ofString( objectMapper.writeValueAsString( body ) ) )
				.build();
	}

	private JsonNode executeRequest(HttpRequest request) throws IOException, InterruptedException {
		HttpResponse<String> response = httpClient.send( request, HttpResponse.BodyHandlers.ofString() );
		if ( response.statusCode() < 200 || response.statusCode() >= 300 ) {
			throw new IOException( "Notion API error " + response.statusCode() + ": " + response.body() );
		}
		return objectMapper.readTree( response.body() );
	}

	private static String nextCursor(JsonNode body) {
		JsonNode hasMore = body.get( "has_more" );
		if ( hasMore != null && hasMore.asBoolean() ) {
			JsonNode next = body.get( "next_cursor" );
			if ( next != null && !next.isNull() ) {
				return next.asText();
			}
		}
		return null;
	}
}
