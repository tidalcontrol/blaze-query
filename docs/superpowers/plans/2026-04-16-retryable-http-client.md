# RetryableHttpClient Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a shared `RetryableHttpClient` to `connector-base` that provides retry with exponential backoff for 429/5xx responses, then migrate all four hand-written HTTP connectors to use it.

**Architecture:** A single new class `RetryableHttpClient` in `connector-base` wraps `java.net.http.HttpClient` with configurable retry, backoff, and timeout behavior. Each connector replaces its raw `HttpClient` with this wrapper. No new dependencies.

**Tech Stack:** Java 17, `java.net.http.HttpClient`, JUnit 5, AssertJ, `com.sun.net.httpserver.HttpServer` (JDK built-in, for tests)

**Spec:** `docs/superpowers/specs/2026-04-16-retryable-http-client-design.md`

---

## File Structure

### New files
- `connector/base/src/main/java/com/blazebit/query/connector/base/RetryableHttpClient.java` -- wrapper class with builder
- `connector/base/src/test/java/com/blazebit/query/connector/base/RetryableHttpClientTest.java` -- tests using JDK HttpServer

### Modified files
- `connector/base/build.gradle` -- add `testImplementation libs.assertj.core`
- `connector/notion/src/main/java/com/blazebit/query/connector/notion/NotionClient.java` -- replace `HttpClient` with `RetryableHttpClient`
- `connector/github-graphql/src/main/java/com/blazebit/query/connector/github/graphql/GitHubGraphQlClient.java` -- replace `HttpClient` + delete `sendWithRetries()`
- `connector/gitlab/src/main/java/com/blazebit/query/connector/gitlab/GitlabGraphQlClient.java` -- replace `HttpClient`
- `connector/observatory/src/main/java/com/blazebit/query/connector/observatory/ObservatoryClient.java` -- replace `HttpClient`, remove per-request timeout
- `connector/notion/build.gradle` -- add dependency on connector-base (if not already present)
- `connector/github-graphql/build.gradle` -- add dependency on connector-base (if not already present)
- `connector/gitlab/build.gradle` -- add dependency on connector-base (if not already present)
- `connector/observatory/build.gradle` -- add dependency on connector-base (if not already present)

---

### Task 1: Write the RetryableHttpClient failing tests

**Files:**
- Modify: `connector/base/build.gradle`
- Create: `connector/base/src/test/java/com/blazebit/query/connector/base/RetryableHttpClientTest.java`

- [ ] **Step 1: Add assertj to connector-base test dependencies**

In `connector/base/build.gradle`, add `assertj.core` to the test dependencies:

```gradle
dependencies {
	api project(':blaze-query-core-api')
	api libs.calcite.core
	testImplementation libs.junit.jupiter
	testImplementation libs.assertj.core
}
```

- [ ] **Step 2: Write the test class with a JDK HttpServer helper and all test methods**

Create `connector/base/src/test/java/com/blazebit/query/connector/base/RetryableHttpClientTest.java`:

```java
/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.base;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RetryableHttpClientTest {

	private HttpServer server;
	private String baseUrl;

	@BeforeEach
	void setUp() throws IOException {
		server = HttpServer.create( new InetSocketAddress( 0 ), 0 );
		server.start();
		baseUrl = "http://localhost:" + server.getAddress().getPort();
	}

	@AfterEach
	void tearDown() {
		server.stop( 0 );
	}

	private void stubResponse(String path, int statusCode, String body) {
		server.createContext( path, exchange -> {
			byte[] bytes = body.getBytes();
			exchange.sendResponseHeaders( statusCode, bytes.length );
			try (OutputStream os = exchange.getResponseBody()) {
				os.write( bytes );
			}
		} );
	}

	private void stubSequence(String path, int[] statusCodes, String body) {
		AtomicInteger call = new AtomicInteger( 0 );
		server.createContext( path, exchange -> {
			int index = Math.min( call.getAndIncrement(), statusCodes.length - 1 );
			byte[] bytes = body.getBytes();
			exchange.sendResponseHeaders( statusCodes[index], bytes.length );
			try (OutputStream os = exchange.getResponseBody()) {
				os.write( bytes );
			}
		} );
	}

	private void stubSequenceWithRetryAfter(String path, int retryAfterSeconds) {
		AtomicInteger call = new AtomicInteger( 0 );
		server.createContext( path, exchange -> {
			int attempt = call.getAndIncrement();
			byte[] bytes = "ok".getBytes();
			if ( attempt == 0 ) {
				exchange.getResponseHeaders().set( "Retry-After", String.valueOf( retryAfterSeconds ) );
				exchange.sendResponseHeaders( 429, bytes.length );
			}
			else {
				exchange.sendResponseHeaders( 200, bytes.length );
			}
			try (OutputStream os = exchange.getResponseBody()) {
				os.write( bytes );
			}
		} );
	}

	private HttpRequest getRequest(String path) {
		return HttpRequest.newBuilder()
				.uri( URI.create( baseUrl + path ) )
				.GET()
				.build();
	}

	@Test
	void should_return_successful_response_without_retry() throws IOException, InterruptedException {
		stubResponse( "/ok", 200, "{\"status\":\"ok\"}" );
		RetryableHttpClient client = RetryableHttpClient.builder().build();

		HttpResponse<String> response = client.send( getRequest( "/ok" ), HttpResponse.BodyHandlers.ofString() );

		assertThat( response.statusCode() ).isEqualTo( 200 );
		assertThat( response.body() ).isEqualTo( "{\"status\":\"ok\"}" );
	}

	@Test
	void should_retry_on_429_and_succeed() throws IOException, InterruptedException {
		stubSequence( "/rate-limit", new int[]{ 429, 200 }, "data" );
		RetryableHttpClient client = RetryableHttpClient.builder()
				.baseDelay( Duration.ofMillis( 50 ) )
				.build();

		HttpResponse<String> response = client.send( getRequest( "/rate-limit" ), HttpResponse.BodyHandlers.ofString() );

		assertThat( response.statusCode() ).isEqualTo( 200 );
	}

	@Test
	void should_retry_on_503_and_succeed() throws IOException, InterruptedException {
		stubSequence( "/unavailable", new int[]{ 503, 200 }, "data" );
		RetryableHttpClient client = RetryableHttpClient.builder()
				.baseDelay( Duration.ofMillis( 50 ) )
				.build();

		HttpResponse<String> response = client.send( getRequest( "/unavailable" ), HttpResponse.BodyHandlers.ofString() );

		assertThat( response.statusCode() ).isEqualTo( 200 );
	}

	@Test
	void should_exhaust_retries_and_return_last_response() throws IOException, InterruptedException {
		stubResponse( "/always-503", 503, "server error" );
		RetryableHttpClient client = RetryableHttpClient.builder()
				.maxRetries( 2 )
				.baseDelay( Duration.ofMillis( 10 ) )
				.build();

		HttpResponse<String> response = client.send( getRequest( "/always-503" ), HttpResponse.BodyHandlers.ofString() );

		assertThat( response.statusCode() ).isEqualTo( 503 );
	}

	@Test
	void should_not_retry_on_400() throws IOException, InterruptedException {
		AtomicInteger callCount = new AtomicInteger( 0 );
		server.createContext( "/bad-request", exchange -> {
			callCount.incrementAndGet();
			byte[] bytes = "bad request".getBytes();
			exchange.sendResponseHeaders( 400, bytes.length );
			try (OutputStream os = exchange.getResponseBody()) {
				os.write( bytes );
			}
		} );
		RetryableHttpClient client = RetryableHttpClient.builder().build();

		HttpResponse<String> response = client.send( getRequest( "/bad-request" ), HttpResponse.BodyHandlers.ofString() );

		assertThat( response.statusCode() ).isEqualTo( 400 );
		assertThat( callCount.get() ).isEqualTo( 1 );
	}

	@Test
	void should_not_retry_on_401() throws IOException, InterruptedException {
		AtomicInteger callCount = new AtomicInteger( 0 );
		server.createContext( "/unauthorized", exchange -> {
			callCount.incrementAndGet();
			byte[] bytes = "unauthorized".getBytes();
			exchange.sendResponseHeaders( 401, bytes.length );
			try (OutputStream os = exchange.getResponseBody()) {
				os.write( bytes );
			}
		} );
		RetryableHttpClient client = RetryableHttpClient.builder().build();

		HttpResponse<String> response = client.send( getRequest( "/unauthorized" ), HttpResponse.BodyHandlers.ofString() );

		assertThat( response.statusCode() ).isEqualTo( 401 );
		assertThat( callCount.get() ).isEqualTo( 1 );
	}

	@Test
	void should_not_retry_on_404() throws IOException, InterruptedException {
		AtomicInteger callCount = new AtomicInteger( 0 );
		server.createContext( "/not-found", exchange -> {
			callCount.incrementAndGet();
			byte[] bytes = "not found".getBytes();
			exchange.sendResponseHeaders( 404, bytes.length );
			try (OutputStream os = exchange.getResponseBody()) {
				os.write( bytes );
			}
		} );
		RetryableHttpClient client = RetryableHttpClient.builder().build();

		HttpResponse<String> response = client.send( getRequest( "/not-found" ), HttpResponse.BodyHandlers.ofString() );

		assertThat( response.statusCode() ).isEqualTo( 404 );
		assertThat( callCount.get() ).isEqualTo( 1 );
	}

	@Test
	void should_respect_retry_after_header() throws IOException, InterruptedException {
		stubSequenceWithRetryAfter( "/retry-after", 1 );
		RetryableHttpClient client = RetryableHttpClient.builder()
				.baseDelay( Duration.ofMillis( 10 ) )
				.build();

		long start = System.currentTimeMillis();
		HttpResponse<String> response = client.send( getRequest( "/retry-after" ), HttpResponse.BodyHandlers.ofString() );
		long elapsed = System.currentTimeMillis() - start;

		assertThat( response.statusCode() ).isEqualTo( 200 );
		// Retry-After says 1 second, so elapsed should be at least ~900ms (allowing jitter)
		assertThat( elapsed ).isGreaterThan( 800 );
	}

	@Test
	void should_retry_correct_number_of_times() throws IOException, InterruptedException {
		AtomicInteger callCount = new AtomicInteger( 0 );
		server.createContext( "/count", exchange -> {
			callCount.incrementAndGet();
			byte[] bytes = "error".getBytes();
			exchange.sendResponseHeaders( 500, bytes.length );
			try (OutputStream os = exchange.getResponseBody()) {
				os.write( bytes );
			}
		} );
		RetryableHttpClient client = RetryableHttpClient.builder()
				.maxRetries( 3 )
				.baseDelay( Duration.ofMillis( 10 ) )
				.build();

		client.send( getRequest( "/count" ), HttpResponse.BodyHandlers.ofString() );

		// 1 initial + 3 retries = 4 total
		assertThat( callCount.get() ).isEqualTo( 4 );
	}

	@Test
	void should_retry_on_500_502_504() throws IOException, InterruptedException {
		for ( int code : new int[]{ 500, 502, 504 } ) {
			AtomicInteger callCount = new AtomicInteger( 0 );
			String path = "/status-" + code;
			server.createContext( path, exchange -> {
				int attempt = callCount.getAndIncrement();
				byte[] bytes = "data".getBytes();
				exchange.sendResponseHeaders( attempt == 0 ? code : 200, bytes.length );
				try (OutputStream os = exchange.getResponseBody()) {
					os.write( bytes );
				}
			} );
			RetryableHttpClient client = RetryableHttpClient.builder()
					.baseDelay( Duration.ofMillis( 10 ) )
					.build();

			HttpResponse<String> response = client.send( getRequest( path ), HttpResponse.BodyHandlers.ofString() );

			assertThat( response.statusCode() ).as( "Expected retry to succeed for status %d", code ).isEqualTo( 200 );
			assertThat( callCount.get() ).as( "Expected 2 calls for status %d", code ).isEqualTo( 2 );
		}
	}

	@Test
	void should_propagate_interrupt_during_backoff() throws IOException {
		stubResponse( "/interrupt", 503, "error" );
		RetryableHttpClient client = RetryableHttpClient.builder()
				.baseDelay( Duration.ofSeconds( 30 ) )
				.build();

		Thread.currentThread().interrupt();
		try {
			client.send( getRequest( "/interrupt" ), HttpResponse.BodyHandlers.ofString() );
		}
		catch (InterruptedException e) {
			assertThat( Thread.interrupted() ).isFalse(); // flag was consumed by the catch
			return;
		}
		// If we get here without InterruptedException, the interrupt flag should be set
		assertThat( Thread.interrupted() ).isTrue();
	}

	@Test
	void should_use_exponential_backoff() throws IOException, InterruptedException {
		stubSequence( "/backoff", new int[]{ 503, 503, 200 }, "data" );
		RetryableHttpClient client = RetryableHttpClient.builder()
				.maxRetries( 3 )
				.baseDelay( Duration.ofMillis( 200 ) )
				.build();

		long start = System.currentTimeMillis();
		HttpResponse<String> response = client.send( getRequest( "/backoff" ), HttpResponse.BodyHandlers.ofString() );
		long elapsed = System.currentTimeMillis() - start;

		assertThat( response.statusCode() ).isEqualTo( 200 );
		// First retry: ~200ms, second retry: ~400ms = ~600ms total minimum (minus jitter)
		assertThat( elapsed ).isGreaterThan( 400 );
	}
}
```

- [ ] **Step 3: Run tests to verify they fail (class not found)**

Run: `./gradlew :blaze-query-connector-base:test --stacktrace 2>&1 | tail -5`
Expected: Compilation failure -- `RetryableHttpClient` does not exist yet.

- [ ] **Step 4: Commit**

```bash
git add connector/base/build.gradle connector/base/src/test/java/com/blazebit/query/connector/base/RetryableHttpClientTest.java
git commit -m "test: add RetryableHttpClient tests (red, class not yet created)"
```

---

### Task 2: Implement RetryableHttpClient

**Files:**
- Create: `connector/base/src/main/java/com/blazebit/query/connector/base/RetryableHttpClient.java`

- [ ] **Step 1: Write the RetryableHttpClient class**

Create `connector/base/src/main/java/com/blazebit/query/connector/base/RetryableHttpClient.java`:

```java
/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.base;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A wrapper around {@link HttpClient} that adds retry with exponential backoff
 * for transient HTTP errors (429, 5xx).
 *
 * <p>All configuration is immutable after construction via the {@link Builder}.
 * Instances are thread-safe and designed to be shared across threads.
 *
 * <p>For multi-tenant deployments, share the underlying {@link HttpClient} across
 * instances via {@link Builder#httpClient(HttpClient)} to avoid creating one
 * thread pool per tenant.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public final class RetryableHttpClient {

	private static final Set<Integer> RETRYABLE_STATUS_CODES = Set.of( 429, 500, 502, 503, 504 );
	private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds( 30 );
	private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds( 60 );
	private static final int DEFAULT_MAX_RETRIES = 3;
	private static final Duration DEFAULT_BASE_DELAY = Duration.ofSeconds( 1 );
	private static final Duration DEFAULT_MAX_DELAY = Duration.ofSeconds( 30 );

	private final HttpClient httpClient;
	private final Duration requestTimeout;
	private final int maxRetries;
	private final long baseDelayMs;
	private final long maxDelayMs;

	private RetryableHttpClient(Builder builder) {
		this.httpClient = builder.httpClient != null
				? builder.httpClient
				: HttpClient.newBuilder()
						.connectTimeout( builder.connectTimeout )
						.build();
		this.requestTimeout = builder.requestTimeout;
		this.maxRetries = builder.maxRetries;
		this.baseDelayMs = builder.baseDelay.toMillis();
		this.maxDelayMs = builder.maxDelay.toMillis();
	}

	/**
	 * Sends an HTTP request, retrying on transient errors (429, 500, 502, 503, 504)
	 * with exponential backoff.
	 *
	 * <p>If the response includes a {@code Retry-After} header with an integer value
	 * (seconds), that value is used instead of the calculated backoff delay.
	 *
	 * @param request the HTTP request to send
	 * @param responseBodyHandler the response body handler
	 * @param <T> the response body type
	 * @return the HTTP response (may be a non-2xx response if retries are exhausted
	 *         or the status is not retryable)
	 * @throws IOException if an I/O error occurs on every attempt
	 * @throws InterruptedException if the thread is interrupted during backoff
	 */
	public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
			throws IOException, InterruptedException {
		HttpRequest timedRequest = withTimeout( request );
		HttpResponse<T> response = null;
		IOException lastException = null;

		for ( int attempt = 0; attempt <= maxRetries; attempt++ ) {
			try {
				response = httpClient.send( timedRequest, responseBodyHandler );
				if ( !RETRYABLE_STATUS_CODES.contains( response.statusCode() ) || attempt == maxRetries ) {
					return response;
				}
				sleep( computeDelay( attempt, response ) );
			}
			catch (IOException e) {
				lastException = e;
				if ( attempt == maxRetries ) {
					throw e;
				}
				sleep( computeDelay( attempt, null ) );
			}
		}

		// Should not be reached, but satisfies the compiler
		if ( response != null ) {
			return response;
		}
		throw lastException;
	}

	private long computeDelay(int attempt, HttpResponse<?> response) {
		// Check Retry-After header on 429 responses
		if ( response != null && response.statusCode() == 429 ) {
			String retryAfter = response.headers().firstValue( "Retry-After" ).orElse( null );
			if ( retryAfter != null ) {
				try {
					long retryAfterMs = Long.parseLong( retryAfter ) * 1000;
					return Math.min( retryAfterMs, maxDelayMs );
				}
				catch (NumberFormatException ignored) {
					// Fall through to exponential backoff
				}
			}
		}

		// Exponential backoff with jitter (+/- 10%)
		long delay = (long) ( baseDelayMs * Math.pow( 2, attempt ) );
		delay = Math.min( delay, maxDelayMs );
		double jitter = 0.9 + ThreadLocalRandom.current().nextDouble() * 0.2;
		return (long) ( delay * jitter );
	}

	private static void sleep(long millis) throws InterruptedException {
		if ( millis > 0 ) {
			Thread.sleep( millis );
		}
	}

	private HttpRequest withTimeout(HttpRequest original) {
		if ( original.timeout().isPresent() ) {
			return original;
		}
		return HttpRequest.newBuilder( original, ( name, value ) -> true )
				.timeout( requestTimeout )
				.build();
	}

	/**
	 * Creates a new {@link Builder}.
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for {@link RetryableHttpClient}.
	 */
	public static final class Builder {

		private HttpClient httpClient;
		private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
		private Duration requestTimeout = DEFAULT_REQUEST_TIMEOUT;
		private int maxRetries = DEFAULT_MAX_RETRIES;
		private Duration baseDelay = DEFAULT_BASE_DELAY;
		private Duration maxDelay = DEFAULT_MAX_DELAY;

		private Builder() {
		}

		/**
		 * Sets an externally-provided {@link HttpClient}. When set, {@link #connectTimeout}
		 * is ignored (the caller configures it on the provided client).
		 * {@link #requestTimeout} still applies per-request.
		 */
		public Builder httpClient(HttpClient httpClient) {
			this.httpClient = httpClient;
			return this;
		}

		/** TCP connect timeout. Only used when no {@link #httpClient} is provided. Default: 30s. */
		public Builder connectTimeout(Duration connectTimeout) {
			this.connectTimeout = connectTimeout;
			return this;
		}

		/** Per-request timeout applied to each attempt. Default: 60s. */
		public Builder requestTimeout(Duration requestTimeout) {
			this.requestTimeout = requestTimeout;
			return this;
		}

		/** Maximum retry attempts after the initial request. Default: 3. */
		public Builder maxRetries(int maxRetries) {
			this.maxRetries = maxRetries;
			return this;
		}

		/** Initial delay before the first retry. Default: 1s. */
		public Builder baseDelay(Duration baseDelay) {
			this.baseDelay = baseDelay;
			return this;
		}

		/** Upper bound on backoff delay. Default: 30s. */
		public Builder maxDelay(Duration maxDelay) {
			this.maxDelay = maxDelay;
			return this;
		}

		/** Builds an immutable {@link RetryableHttpClient}. */
		public RetryableHttpClient build() {
			return new RetryableHttpClient( this );
		}
	}
}
```

- [ ] **Step 2: Run tests to verify they pass**

Run: `./gradlew :blaze-query-connector-base:test -Plog-test-progress=true --stacktrace 2>&1 | tail -15`
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 3: Run spotless check**

Run: `./gradlew :blaze-query-connector-base:spotlessCheck --stacktrace 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add connector/base/src/main/java/com/blazebit/query/connector/base/RetryableHttpClient.java
git commit -m "feat: add RetryableHttpClient with retry, exponential backoff, and Retry-After support"
```

---

### Task 3: Migrate Notion connector

**Files:**
- Modify: `connector/notion/build.gradle`
- Modify: `connector/notion/src/main/java/com/blazebit/query/connector/notion/NotionClient.java`

- [ ] **Step 1: Verify connector-base dependency exists in Notion build.gradle**

Read `connector/notion/build.gradle`. If `api project(':blaze-query-connector-base')` is already present, skip to step 2. If not, add it to the dependencies block.

- [ ] **Step 2: Replace HttpClient with RetryableHttpClient in NotionClient**

In `connector/notion/src/main/java/com/blazebit/query/connector/notion/NotionClient.java`:

Replace the import:
```java
// Remove:
import java.net.http.HttpClient;
// Add:
import com.blazebit.query.connector.base.RetryableHttpClient;
```

Replace the field declaration (line 53):
```java
// Old:
private final HttpClient httpClient;
// New:
private final RetryableHttpClient httpClient;
```

Replace the constructor body (lines 61-65):
```java
public NotionClient(String apiToken) {
	this.apiToken = apiToken;
	this.httpClient = RetryableHttpClient.builder().build();
	this.objectMapper = new ObjectMapper();
}
```

Replace the `executeRequest` method (lines 326-332). The method stays the same -- `RetryableHttpClient.send()` has the same signature as `HttpClient.send()`. The status code check stays because it handles non-retryable errors (400, 401, 403, 404) that the wrapper returns without retrying:
```java
private JsonNode executeRequest(HttpRequest request) throws IOException, InterruptedException {
	HttpResponse<String> response = httpClient.send( request, HttpResponse.BodyHandlers.ofString() );
	if ( response.statusCode() < 200 || response.statusCode() >= 300 ) {
		throw new IOException( "Notion API error " + response.statusCode() + ": " + response.body() );
	}
	return objectMapper.readTree( response.body() );
}
```

- [ ] **Step 3: Run Notion connector tests**

Run: `./gradlew :blaze-query-connector-notion:check -Plog-test-progress=true --stacktrace 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL, all 27 tests pass.

- [ ] **Step 4: Commit**

```bash
git add connector/notion/build.gradle connector/notion/src/main/java/com/blazebit/query/connector/notion/NotionClient.java
git commit -m "refactor: migrate NotionClient to RetryableHttpClient"
```

---

### Task 4: Migrate GitHub GraphQL connector

**Files:**
- Modify: `connector/github-graphql/build.gradle`
- Modify: `connector/github-graphql/src/main/java/com/blazebit/query/connector/github/graphql/GitHubGraphQlClient.java`

- [ ] **Step 1: Add connector-base dependency to GitHub GraphQL build.gradle**

Read `connector/github-graphql/build.gradle`. If `api project(':blaze-query-connector-base')` is not present, add it to the dependencies block.

- [ ] **Step 2: Replace HttpClient and remove sendWithRetries in GitHubGraphQlClient**

In `connector/github-graphql/src/main/java/com/blazebit/query/connector/github/graphql/GitHubGraphQlClient.java`:

Replace the import:
```java
// Remove:
import java.net.http.HttpClient;
// Add:
import com.blazebit.query.connector.base.RetryableHttpClient;
```

Replace the field declaration (line 37):
```java
// Old:
private final HttpClient httpClient;
// New:
private final RetryableHttpClient httpClient;
```

Delete the retry constants (lines 34-35):
```java
// Delete these two lines:
private static final int MAX_RETRIES = 3;
private static final long RETRY_BASE_DELAY_MS = 1000L;
```

Replace the constructor (lines 45-49):
```java
GitHubGraphQlClient(String authToken, String endpoint) {
	this.httpClient = RetryableHttpClient.builder().build();
	this.endpoint = endpoint;
	this.authToken = authToken;
}
```

Replace the call site in `executePaginatedQuery` (line 461):
```java
// Old:
HttpResponse<String> response = sendWithRetries( request, rootNode );
// New:
HttpResponse<String> response = httpClient.send( request, HttpResponse.BodyHandlers.ofString() );
```

Add error handling after the new `send` call (previously handled inside `sendWithRetries`). Insert after the `httpClient.send()` line:
```java
HttpResponse<String> response = httpClient.send( request, HttpResponse.BodyHandlers.ofString() );
if ( response.statusCode() != 200 ) {
	throw new RuntimeException( "GitHub API error " + response.statusCode() + ": " + response.body() );
}
```

Delete the entire `sendWithRetries` method (lines 513-540).

Delete the `isRetryableStatus` method (lines 542-544).

- [ ] **Step 3: Run GitHub GraphQL connector tests**

Run: `./gradlew :blaze-query-connector-github-graphql:check -Plog-test-progress=true --stacktrace 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add connector/github-graphql/build.gradle connector/github-graphql/src/main/java/com/blazebit/query/connector/github/graphql/GitHubGraphQlClient.java
git commit -m "refactor: migrate GitHubGraphQlClient to RetryableHttpClient, remove hand-rolled retry"
```

---

### Task 5: Migrate GitLab GraphQL connector

**Files:**
- Modify: `connector/gitlab/build.gradle`
- Modify: `connector/gitlab/src/main/java/com/blazebit/query/connector/gitlab/GitlabGraphQlClient.java`

- [ ] **Step 1: Add connector-base dependency to GitLab build.gradle**

Read `connector/gitlab/build.gradle`. If `api project(':blaze-query-connector-base')` is not present, add it to the dependencies block.

- [ ] **Step 2: Replace HttpClient with RetryableHttpClient in GitlabGraphQlClient**

In `connector/gitlab/src/main/java/com/blazebit/query/connector/gitlab/GitlabGraphQlClient.java`:

Replace the import:
```java
// Remove:
import java.net.http.HttpClient;
// Add:
import com.blazebit.query.connector.base.RetryableHttpClient;
```

Replace the field declaration (line 30):
```java
// Old:
private final HttpClient httpClient;
// New:
private final RetryableHttpClient httpClient;
```

Replace the constructor (lines 34-38):
```java
public GitlabGraphQlClient(String host, String gitlabToken) {
	this.httpClient = RetryableHttpClient.builder().build();
	this.gitlabApiUrl = host + "/api/graphql";
	this.authToken = gitlabToken;
}
```

The `httpClient.send()` call sites at lines 358 and 398 do not change -- `RetryableHttpClient.send()` has the same signature.

- [ ] **Step 3: Run GitLab connector tests**

Run: `./gradlew :blaze-query-connector-gitlab:check -Plog-test-progress=true --stacktrace 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add connector/gitlab/build.gradle connector/gitlab/src/main/java/com/blazebit/query/connector/gitlab/GitlabGraphQlClient.java
git commit -m "refactor: migrate GitlabGraphQlClient to RetryableHttpClient"
```

---

### Task 6: Migrate Observatory connector

**Files:**
- Modify: `connector/observatory/build.gradle`
- Modify: `connector/observatory/src/main/java/com/blazebit/query/connector/observatory/ObservatoryClient.java`

- [ ] **Step 1: Add connector-base dependency to Observatory build.gradle**

Read `connector/observatory/build.gradle`. If `api project(':blaze-query-connector-base')` is not present, add it to the dependencies block.

- [ ] **Step 2: Replace HttpClient with RetryableHttpClient in ObservatoryClient**

In `connector/observatory/src/main/java/com/blazebit/query/connector/observatory/ObservatoryClient.java`:

Replace the import:
```java
// Remove:
import java.net.http.HttpClient;
// Add:
import com.blazebit.query.connector.base.RetryableHttpClient;
```

Replace the field declaration (line 29):
```java
// Old:
private final HttpClient httpClient;
// New:
private final RetryableHttpClient httpClient;
```

Replace the constructor (lines 35-41):
```java
public ObservatoryClient(String host, String baseUrl) {
	this.host = host;
	this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
	this.httpClient = RetryableHttpClient.builder()
			.connectTimeout(Duration.ofSeconds(30))
			.requestTimeout(Duration.ofSeconds(60))
			.build();
}
```

Remove the per-request timeout from the `runScan` method (line 68). The `RetryableHttpClient` applies `requestTimeout` automatically. Change the `HttpRequest` builder in `runScan()`:
```java
HttpRequest httpRequest = HttpRequest.newBuilder()
		.uri(uri)
		.header("Accept", "application/json")
		.POST(HttpRequest.BodyPublishers.noBody())
		.build();
```

- [ ] **Step 3: Run Observatory connector tests**

Run: `./gradlew :blaze-query-connector-observatory:check -Plog-test-progress=true --stacktrace 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add connector/observatory/build.gradle connector/observatory/src/main/java/com/blazebit/query/connector/observatory/ObservatoryClient.java
git commit -m "refactor: migrate ObservatoryClient to RetryableHttpClient, consolidate timeout config"
```

---

### Task 7: Final verification

**Files:** None (verification only)

- [ ] **Step 1: Run full build for all affected modules**

Run: `./gradlew :blaze-query-connector-base:check :blaze-query-connector-notion:check :blaze-query-connector-github-graphql:check :blaze-query-connector-gitlab:check :blaze-query-connector-observatory:check -Plog-test-progress=true --stacktrace 2>&1 | tail -15`
Expected: BUILD SUCCESSFUL for all modules.

- [ ] **Step 2: Run spotless across all affected modules**

Run: `./gradlew :blaze-query-connector-base:spotlessCheck :blaze-query-connector-notion:spotlessCheck :blaze-query-connector-github-graphql:spotlessCheck :blaze-query-connector-gitlab:spotlessCheck :blaze-query-connector-observatory:spotlessCheck --stacktrace 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Verify no raw HttpClient.newHttpClient() remains in migrated connectors**

Run: `grep -r "HttpClient.newHttpClient\|HttpClient.newBuilder" connector/notion connector/github-graphql connector/gitlab connector/observatory --include="*.java"`
Expected: No output (all usages replaced).
