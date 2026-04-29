# Shared RetryableHttpClient for connector-base

**Date:** 2026-04-16
**Status:** Approved
**Scope:** `connector-base` module + migration of all four hand-written HTTP connectors

## Problem

Four connectors in blaze-query use `java.net.http.HttpClient` directly: Notion, GitHub GraphQL, GitLab GraphQL, and Observatory. Each has inconsistent (or absent) handling of:

- **Rate limiting (HTTP 429):** Only GitHub GraphQL retries on 429. Notion, GitLab, and Observatory do not, despite calling APIs that enforce rate limits.
- **Transient server errors (5xx):** Only GitHub GraphQL retries on 5xx. The others treat all non-2xx as terminal failures.
- **Timeouts:** Only Observatory configures timeouts (30s connect, 60s request). The others use JDK defaults (unbounded).
- **Backoff:** GitHub GraphQL implements linear backoff (1s, 2s, 3s). There is no exponential backoff anywhere.

SDK-based connectors (AWS, Datadog) are not affected -- their SDKs handle retry internally.

## Solution

A single new class in `connector-base`: `RetryableHttpClient`. It wraps `java.net.http.HttpClient` and adds retry with exponential backoff, timeout configuration, and `Retry-After` header support. All four hand-written connectors migrate to use it.

No external dependencies are added. The retry logic is ~40 lines of purpose-built code.

## API

### Construction

```java
// Minimal -- all defaults
RetryableHttpClient client = RetryableHttpClient.builder().build();

// Customized
RetryableHttpClient client = RetryableHttpClient.builder()
    .connectTimeout(Duration.ofSeconds(30))    // default 30s
    .requestTimeout(Duration.ofSeconds(60))    // default 60s
    .maxRetries(5)                             // default 3
    .baseDelay(Duration.ofSeconds(1))          // default 1s
    .maxDelay(Duration.ofSeconds(30))          // default 30s
    .build();

// With externally-provided HttpClient (for multi-tenant / shared connection pool)
HttpClient sharedTransport = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(30))
    .build();

RetryableHttpClient client = RetryableHttpClient.builder()
    .httpClient(sharedTransport)
    .maxRetries(5)
    .build();
```

### Usage

```java
// Mirrors HttpClient.send() signature
HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
```

### Builder parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `httpClient` | `HttpClient` | Created internally | Optional externally-provided HTTP transport. When provided, `connectTimeout` is ignored (caller configures it on the provided client). `requestTimeout` still applies -- it is set per-request, not on the client. |
| `connectTimeout` | `Duration` | 30s | TCP connect timeout. Only used when `httpClient` is not provided. |
| `requestTimeout` | `Duration` | 60s | Per-request timeout. Applied to each individual attempt, not the total retry duration. |
| `maxRetries` | `int` | 3 | Maximum number of retry attempts after the initial request. Total attempts = `maxRetries + 1`. |
| `baseDelay` | `Duration` | 1s | Initial delay before the first retry. |
| `maxDelay` | `Duration` | 30s | Upper bound on backoff delay. |

## Retry behavior

### Retryable status codes

429, 500, 502, 503, 504.

All other status codes are returned to the caller immediately (including 400, 401, 403, 404).

### Backoff strategy

Exponential backoff with jitter:

```
delay = min(baseDelay * 2^(attempt - 1), maxDelay) * (0.9 + random * 0.2)
```

- Attempt 1: ~1s
- Attempt 2: ~2s
- Attempt 3: ~4s
- Capped at `maxDelay` (default 30s)
- Jitter: +/-10% randomization to prevent thundering herd across concurrent callers

### Retry-After header

On 429 responses, if the response includes a `Retry-After` header with an integer value (seconds), that value is used instead of the calculated backoff. The `maxDelay` cap still applies.

`Retry-After` with HTTP-date format is not supported (no current Notion/GitHub/GitLab API uses it).

### InterruptedException handling

If `Thread.sleep()` is interrupted during backoff, the thread interrupt flag is restored and the exception is propagated as an `IOException` wrapping the `InterruptedException`. This matches the `HttpClient.send()` contract and preserves correct interrupt semantics for virtual threads and cancellation.

### What is NOT retried

- Connection exceptions (`IOException` from `HttpClient.send()`): These are genuine network failures, not transient API errors. Retrying a DNS resolution failure or connection refused is unlikely to help.
- Non-retryable HTTP status codes (4xx other than 429): These are client errors that won't change on retry.

## Thread safety

`RetryableHttpClient` is immutable after construction. All fields are final. Retry state (attempt counter, current delay) is local to each `send()` invocation. The underlying `java.net.http.HttpClient` is documented as thread-safe. Multiple threads can share a single `RetryableHttpClient` instance.

### Multi-tenant usage

The default builder creates a fresh `HttpClient` per `RetryableHttpClient` instance. For multi-tenant deployments with many connector clients, callers should share an `HttpClient` via the `httpClient()` builder method to avoid creating one thread pool per tenant:

```java
HttpClient sharedTransport = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(30))
    .build();

// Each tenant gets its own RetryableHttpClient (can vary retry config)
// but shares the underlying transport
RetryableHttpClient tenantA = RetryableHttpClient.builder()
    .httpClient(sharedTransport)
    .maxRetries(5)
    .build();
```

### Virtual threads

The design is compatible with virtual threads (Java 21+). `HttpClient.send()` and `Thread.sleep()` both unmount virtual threads from carrier threads during blocking. No `synchronized` blocks, no thread pinning, no `ThreadLocal` usage. Callers on Java 21+ can pass an `HttpClient` configured with a virtual thread executor.

## Connector migration

### Notion (`NotionClient`)

- Replace `HttpClient.newHttpClient()` with `RetryableHttpClient.builder().build()`
- Replace `httpClient.send(request, BodyHandlers.ofString())` calls with `retryableClient.send(request, BodyHandlers.ofString())`
- Keep the status code check in `executeRequest()` for non-retryable errors (400, 401, 403, 404). After the wrapper has exhausted retries, any non-2xx response that reaches `executeRequest()` is a genuine failure.
- Remove: nothing (Notion had no retry logic to delete)

### GitHub GraphQL (`GitHubGraphQlClient`)

- Replace `HttpClient.newHttpClient()` with `RetryableHttpClient.builder().build()`
- Delete `sendWithRetries()` method entirely
- Delete `MAX_RETRIES` and `RETRY_BASE_DELAY_MS` constants
- Callers of `sendWithRetries()` switch to plain `send()` calls
- Remove: ~30 lines of hand-rolled retry logic

### GitLab GraphQL (`GitlabGraphQlClient`)

- Replace `HttpClient.newHttpClient()` with `RetryableHttpClient.builder().build()`
- Remove: nothing (GitLab had no retry logic)

### Observatory (`ObservatoryClient`)

- Replace `HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build()` with `RetryableHttpClient.builder().connectTimeout(Duration.ofSeconds(30)).requestTimeout(Duration.ofSeconds(60)).build()`
- Remove per-request `HttpRequest.timeout()` calls -- the wrapper applies `requestTimeout` to each attempt
- Remove: timeout configuration split across HttpClient and HttpRequest

### Not migrated

- **AWS connectors:** Use AWS SDK's `SdkHttpClient` with built-in retry. No change.
- **Datadog connector:** Uses Datadog SDK's `ApiClient` with built-in retry. No change.
- **Jira / Kandji / GitHub REST connectors:** Use generated OpenAPI clients. No change.

## File layout

```
connector/base/src/main/java/com/blazebit/query/connector/base/
    RetryableHttpClient.java      # New: wrapper class with builder
```

No new modules, no new dependencies, no new `META-INF/services` registrations.

## Testing

Unit tests for `RetryableHttpClient` in `connector/base/src/test/`:

- Successful request on first attempt (no retry)
- Retry on 429, verify correct number of attempts
- Retry on 503, verify exponential backoff delays
- Respect `Retry-After` header value on 429
- Exhaust retries and return final error response
- Non-retryable status (400, 401, 404) returned immediately without retry
- Request timeout applied per attempt
- Thread interrupt during backoff propagates correctly

Tests use a local HTTP server (e.g., `com.sun.net.httpserver.HttpServer` from the JDK) to simulate responses. No external test dependencies needed beyond JUnit and AssertJ (already in the test classpath).

## Alternatives considered

### Pull in Failsafe or Resilience4j

Both are mature retry libraries with composable policies. Rejected because:
- Adds a transitive dependency to every connector consumer
- `connector-base` currently has zero utility dependencies beyond Jackson
- We need ~40 lines of retry logic, not a policy composition framework
- No anticipated need for circuit breakers, bulkheads, or fallback chains

### Retry at the DataFetcher level

Wrap `DataFetcher.fetch()` with retry logic instead of wrapping HTTP calls. Rejected because retrying at the fetch level restarts pagination from scratch -- a 429 on page 50 of 100 would discard all progress and start over.

### Do nothing, document the limitation

The Notion connector's CLAUDE.md already documents the lack of retry as a known limitation. Rejected because three of four hand-written connectors have the same gap, and the fix is straightforward.
