/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.base;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Max Hovens
 * @since 2.4.4
 */
class RetryableHttpClientTest {

	private HttpServer server;

	@AfterEach
	void tearDown() {
		if (server != null) {
			server.stop(0);
		}
	}

	private HttpServer startServer(int port, com.sun.net.httpserver.HttpHandler handler) throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
		server.createContext("/", handler);
		server.start();
		return server;
	}

	private URI serverUri(HttpServer server) {
		return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
	}

	@Test
	void should_return_successful_response_without_retry() throws Exception {
		AtomicInteger callCount = new AtomicInteger();
		HttpServer srv = startServer(0, exchange -> {
			callCount.incrementAndGet();
			exchange.sendResponseHeaders(200, -1);
			exchange.close();
		});

		RetryableHttpClient client = RetryableHttpClient.builder()
				.baseDelay(Duration.ofMillis(10))
				.build();

		HttpRequest request = HttpRequest.newBuilder(serverUri(srv)).GET().build();
		HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(callCount.get()).isEqualTo(1);
	}

	@Test
	void should_retry_on_429_and_succeed() throws Exception {
		AtomicInteger callCount = new AtomicInteger();
		HttpServer srv = startServer(0, exchange -> {
			int call = callCount.incrementAndGet();
			if (call == 1) {
				exchange.sendResponseHeaders(429, -1);
			}
			else {
				exchange.sendResponseHeaders(200, -1);
			}
			exchange.close();
		});

		RetryableHttpClient client = RetryableHttpClient.builder()
				.baseDelay(Duration.ofMillis(10))
				.build();

		HttpRequest request = HttpRequest.newBuilder(serverUri(srv)).GET().build();
		HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(callCount.get()).isEqualTo(2);
	}

	@Test
	void should_retry_on_503_and_succeed() throws Exception {
		AtomicInteger callCount = new AtomicInteger();
		HttpServer srv = startServer(0, exchange -> {
			int call = callCount.incrementAndGet();
			if (call == 1) {
				exchange.sendResponseHeaders(503, -1);
			}
			else {
				exchange.sendResponseHeaders(200, -1);
			}
			exchange.close();
		});

		RetryableHttpClient client = RetryableHttpClient.builder()
				.baseDelay(Duration.ofMillis(10))
				.build();

		HttpRequest request = HttpRequest.newBuilder(serverUri(srv)).GET().build();
		HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(callCount.get()).isEqualTo(2);
	}

	@Test
	void should_exhaust_retries_and_return_last_response() throws Exception {
		AtomicInteger callCount = new AtomicInteger();
		HttpServer srv = startServer(0, exchange -> {
			callCount.incrementAndGet();
			exchange.sendResponseHeaders(503, -1);
			exchange.close();
		});

		RetryableHttpClient client = RetryableHttpClient.builder()
				.maxRetries(2)
				.baseDelay(Duration.ofMillis(10))
				.build();

		HttpRequest request = HttpRequest.newBuilder(serverUri(srv)).GET().build();
		HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

		assertThat(response.statusCode()).isEqualTo(503);
		assertThat(callCount.get()).isEqualTo(3); // 1 initial + 2 retries
	}

	@Test
	void should_not_retry_on_400() throws Exception {
		AtomicInteger callCount = new AtomicInteger();
		HttpServer srv = startServer(0, exchange -> {
			callCount.incrementAndGet();
			exchange.sendResponseHeaders(400, -1);
			exchange.close();
		});

		RetryableHttpClient client = RetryableHttpClient.builder()
				.baseDelay(Duration.ofMillis(10))
				.build();

		HttpRequest request = HttpRequest.newBuilder(serverUri(srv)).GET().build();
		HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

		assertThat(response.statusCode()).isEqualTo(400);
		assertThat(callCount.get()).isEqualTo(1);
	}

	@Test
	void should_not_retry_on_401() throws Exception {
		AtomicInteger callCount = new AtomicInteger();
		HttpServer srv = startServer(0, exchange -> {
			callCount.incrementAndGet();
			exchange.sendResponseHeaders(401, -1);
			exchange.close();
		});

		RetryableHttpClient client = RetryableHttpClient.builder()
				.baseDelay(Duration.ofMillis(10))
				.build();

		HttpRequest request = HttpRequest.newBuilder(serverUri(srv)).GET().build();
		HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

		assertThat(response.statusCode()).isEqualTo(401);
		assertThat(callCount.get()).isEqualTo(1);
	}

	@Test
	void should_not_retry_on_404() throws Exception {
		AtomicInteger callCount = new AtomicInteger();
		HttpServer srv = startServer(0, exchange -> {
			callCount.incrementAndGet();
			exchange.sendResponseHeaders(404, -1);
			exchange.close();
		});

		RetryableHttpClient client = RetryableHttpClient.builder()
				.baseDelay(Duration.ofMillis(10))
				.build();

		HttpRequest request = HttpRequest.newBuilder(serverUri(srv)).GET().build();
		HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

		assertThat(response.statusCode()).isEqualTo(404);
		assertThat(callCount.get()).isEqualTo(1);
	}

	@Test
	void should_respect_retry_after_header() throws Exception {
		AtomicInteger callCount = new AtomicInteger();
		HttpServer srv = startServer(0, exchange -> {
			int call = callCount.incrementAndGet();
			if (call == 1) {
				exchange.getResponseHeaders().add("Retry-After", "1");
				exchange.sendResponseHeaders(429, -1);
			}
			else {
				exchange.sendResponseHeaders(200, -1);
			}
			exchange.close();
		});

		RetryableHttpClient client = RetryableHttpClient.builder()
				.baseDelay(Duration.ofMillis(10))
				.build();

		HttpRequest request = HttpRequest.newBuilder(serverUri(srv)).GET().build();
		long start = System.currentTimeMillis();
		HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
		long elapsed = System.currentTimeMillis() - start;

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(callCount.get()).isEqualTo(2);
		assertThat(elapsed).isGreaterThanOrEqualTo(800L);
	}

	@Test
	void should_retry_correct_number_of_times() throws Exception {
		AtomicInteger callCount = new AtomicInteger();
		HttpServer srv = startServer(0, exchange -> {
			callCount.incrementAndGet();
			exchange.sendResponseHeaders(500, -1);
			exchange.close();
		});

		RetryableHttpClient client = RetryableHttpClient.builder()
				.maxRetries(3)
				.baseDelay(Duration.ofMillis(10))
				.build();

		HttpRequest request = HttpRequest.newBuilder(serverUri(srv)).GET().build();
		HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

		assertThat(response.statusCode()).isEqualTo(500);
		assertThat(callCount.get()).isEqualTo(4); // 1 initial + 3 retries
	}

	@Test
	void should_retry_on_500_502_504() throws Exception {
		for (int retryableCode : new int[]{500, 502, 504}) {
			AtomicInteger callCount = new AtomicInteger();
			if (server != null) {
				server.stop(0);
			}
			HttpServer srv = startServer(0, exchange -> {
				int call = callCount.incrementAndGet();
				if (call == 1) {
					exchange.sendResponseHeaders(retryableCode, -1);
				}
				else {
					exchange.sendResponseHeaders(200, -1);
				}
				exchange.close();
			});

			RetryableHttpClient client = RetryableHttpClient.builder()
					.baseDelay(Duration.ofMillis(10))
					.build();

			HttpRequest request = HttpRequest.newBuilder(serverUri(srv)).GET().build();
			HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

			assertThat(response.statusCode())
					.as("Expected retry to succeed for status code %d", retryableCode)
					.isEqualTo(200);
			assertThat(callCount.get())
					.as("Expected 2 calls for status code %d", retryableCode)
					.isEqualTo(2);
		}
	}

	@Test
	void should_propagate_interrupt_during_backoff() throws Exception {
		AtomicInteger callCount = new AtomicInteger();
		HttpServer srv = startServer(0, exchange -> {
			callCount.incrementAndGet();
			exchange.sendResponseHeaders(503, -1);
			exchange.close();
		});

		RetryableHttpClient client = RetryableHttpClient.builder()
				.maxRetries(3)
				.baseDelay(Duration.ofMillis(500))
				.build();

		HttpRequest request = HttpRequest.newBuilder(serverUri(srv)).GET().build();

		Thread testThread = Thread.currentThread();
		// Schedule an interrupt shortly after the first request completes
		new Thread(() -> {
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException e) {
				// ignore
			}
			testThread.interrupt();
		}).start();

		try {
			client.send(request, HttpResponse.BodyHandlers.discarding());
			// If we get here, the interrupt flag should be set
			assertThat(Thread.interrupted()).isTrue();
		}
		catch (InterruptedException e) {
			// This is also acceptable: the InterruptedException propagated
			assertThat(e).isInstanceOf(InterruptedException.class);
		}
	}

	@Test
	void should_use_exponential_backoff() throws Exception {
		AtomicInteger callCount = new AtomicInteger();
		HttpServer srv = startServer(0, exchange -> {
			int call = callCount.incrementAndGet();
			if (call <= 2) {
				exchange.sendResponseHeaders(503, -1);
			}
			else {
				exchange.sendResponseHeaders(200, -1);
			}
			exchange.close();
		});

		RetryableHttpClient client = RetryableHttpClient.builder()
				.baseDelay(Duration.ofMillis(200))
				.build();

		HttpRequest request = HttpRequest.newBuilder(serverUri(srv)).GET().build();
		long start = System.currentTimeMillis();
		HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
		long elapsed = System.currentTimeMillis() - start;

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(callCount.get()).isEqualTo(3);
		// First retry: ~200ms, second retry: ~400ms, total >= 400ms (with jitter floor at 0.9)
		assertThat(elapsed).isGreaterThanOrEqualTo(400L);
	}
}
