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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * An HTTP client wrapper that retries requests on transient server errors
 * (429, 500, 502, 503, 504) with exponential backoff and jitter.
 *
 * <p>Instances are immutable and thread-safe after construction.
 * Use {@link #builder()} to create a new instance.</p>
 *
 * @author Max Hovens
 * @since 2.4.4
 */
public final class RetryableHttpClient {

	private static final Set<Integer> RETRYABLE_STATUS_CODES = Set.of(429, 500, 502, 503, 504);

	private final HttpClient httpClient;
	private final Duration requestTimeout;
	private final int maxRetries;
	private final Duration baseDelay;
	private final Duration maxDelay;

	private RetryableHttpClient(Builder builder) {
		if (builder.httpClient != null) {
			this.httpClient = builder.httpClient;
		}
		else {
			this.httpClient = HttpClient.newBuilder()
					.connectTimeout(builder.connectTimeout)
					.build();
		}
		this.requestTimeout = builder.requestTimeout;
		this.maxRetries = builder.maxRetries;
		this.baseDelay = builder.baseDelay;
		this.maxDelay = builder.maxDelay;
	}

	/**
	 * Creates a new builder for {@link RetryableHttpClient}.
	 *
	 * @return a new builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Sends an HTTP request, retrying on transient failures with exponential backoff.
	 *
	 * @param request     the HTTP request to send
	 * @param bodyHandler the response body handler
	 * @param <T>         the response body type
	 * @return the HTTP response
	 * @throws IOException          if an I/O error occurs
	 * @throws InterruptedException if the thread is interrupted during backoff
	 */
	public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> bodyHandler)
			throws IOException, InterruptedException {
		HttpRequest effectiveRequest = applyTimeout(request);
		HttpResponse<T> response = null;

		for (int attempt = 0; attempt <= maxRetries; attempt++) {
			response = httpClient.send(effectiveRequest, bodyHandler);
			int statusCode = response.statusCode();

			if (!RETRYABLE_STATUS_CODES.contains(statusCode) || attempt == maxRetries) {
				return response;
			}

			long delayMs = computeDelay(statusCode, response, attempt);
			Thread.sleep(delayMs);
		}

		return response;
	}

	private HttpRequest applyTimeout(HttpRequest request) {
		if (request.timeout().isPresent()) {
			return request;
		}
		return HttpRequest.newBuilder(request, (name, value) -> true)
				.timeout(requestTimeout)
				.build();
	}

	private <T> long computeDelay(int statusCode, HttpResponse<T> response, int attempt) {
		long delayMs;

		if (statusCode == 429) {
			Optional<String> retryAfter = response.headers().firstValue("retry-after");
			if (retryAfter.isPresent()) {
				try {
					long retryAfterSeconds = Long.parseLong(retryAfter.get().trim());
					delayMs = Math.min(retryAfterSeconds * 1000, maxDelay.toMillis());
					return applyJitter(delayMs);
				}
				catch (NumberFormatException e) {
					// Fall through to exponential backoff
				}
			}
		}

		delayMs = baseDelay.toMillis() * (1L << attempt);
		delayMs = Math.min(delayMs, maxDelay.toMillis());
		return applyJitter(delayMs);
	}

	private long applyJitter(long delayMs) {
		double jitter = 0.9 + ThreadLocalRandom.current().nextDouble() * 0.2;
		return Math.max(1, (long) (delayMs * jitter));
	}

	/**
	 * Builder for {@link RetryableHttpClient}.
	 */
	public static final class Builder {

		private HttpClient httpClient;
		private Duration connectTimeout = Duration.ofSeconds(30);
		private Duration requestTimeout = Duration.ofSeconds(60);
		private int maxRetries = 3;
		private Duration baseDelay = Duration.ofSeconds(1);
		private Duration maxDelay = Duration.ofSeconds(30);

		private Builder() {
		}

		/**
		 * Sets an external HTTP client to use. When provided, {@link #connectTimeout(Duration)} is ignored.
		 *
		 * @param httpClient the HTTP client
		 * @return this builder
		 */
		public Builder httpClient(HttpClient httpClient) {
			this.httpClient = httpClient;
			return this;
		}

		/**
		 * Sets the connection timeout. Default is 30 seconds.
		 * Only used when no external {@link HttpClient} is provided.
		 *
		 * @param connectTimeout the connection timeout
		 * @return this builder
		 */
		public Builder connectTimeout(Duration connectTimeout) {
			this.connectTimeout = connectTimeout;
			return this;
		}

		/**
		 * Sets the per-request timeout. Default is 60 seconds.
		 * Applied to each request attempt if the request does not already have a timeout.
		 *
		 * @param requestTimeout the request timeout
		 * @return this builder
		 */
		public Builder requestTimeout(Duration requestTimeout) {
			this.requestTimeout = requestTimeout;
			return this;
		}

		/**
		 * Sets the maximum number of retries after the initial attempt. Default is 3.
		 *
		 * @param maxRetries the maximum number of retries
		 * @return this builder
		 */
		public Builder maxRetries(int maxRetries) {
			this.maxRetries = maxRetries;
			return this;
		}

		/**
		 * Sets the base delay for exponential backoff. Default is 1 second.
		 *
		 * @param baseDelay the base delay
		 * @return this builder
		 */
		public Builder baseDelay(Duration baseDelay) {
			this.baseDelay = baseDelay;
			return this;
		}

		/**
		 * Sets the maximum delay cap for backoff. Default is 30 seconds.
		 *
		 * @param maxDelay the maximum delay
		 * @return this builder
		 */
		public Builder maxDelay(Duration maxDelay) {
			this.maxDelay = maxDelay;
			return this;
		}

		/**
		 * Builds the {@link RetryableHttpClient}.
		 *
		 * @return a new RetryableHttpClient instance
		 */
		public RetryableHttpClient build() {
			return new RetryableHttpClient(this);
		}
	}
}
