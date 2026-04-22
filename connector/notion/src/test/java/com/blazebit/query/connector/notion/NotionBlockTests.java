/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.notion;

import com.blazebit.query.QueryContext;
import com.blazebit.query.TypeReference;
import com.blazebit.query.impl.QueryContextBuilderImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class NotionBlockTests {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new NotionSchemaProvider() );
		builder.registerSchemaObjectAlias( NotionBlock.class, "NotionBlock" );
		CONTEXT = builder.build();
	}

	private static NotionBlock paragraph() {
		return new NotionBlock(
				"block-001", "page-001", "paragraph",
				"2024-01-10T09:00:00.000Z", "2024-01-10T09:00:00.000Z",
				"user-001", "user-001", false, false,
				"This document contains the quarterly security review." );
	}

	private static NotionBlock sensitiveBlock() {
		return new NotionBlock(
				"block-002", "page-001", "paragraph",
				"2024-01-11T10:00:00.000Z", "2024-01-11T10:00:00.000Z",
				"user-002", "user-002", false, false,
				"API_KEY=sk-prod-abc123 please keep secret" );
	}

	private static NotionBlock toggleWithChildren() {
		return new NotionBlock(
				"block-003", "page-002", "toggle",
				"2024-02-01T08:00:00.000Z", "2024-02-01T08:00:00.000Z",
				"user-001", "user-001", true, false,
				"Confidential: internal salary bands" );
	}

	private static NotionBlock imageBlock() {
		return new NotionBlock(
				"block-004", "page-002", "image",
				"2024-02-05T09:00:00.000Z", "2024-02-05T09:00:00.000Z",
				"user-003", "user-003", false, false, null );
	}

	private static NotionBlock trashedBlock() {
		return new NotionBlock(
				"block-005", "page-003", "paragraph",
				"2023-06-01T00:00:00.000Z", "2023-06-01T00:00:00.000Z",
				"user-001", "user-001", false, true,
				"Deleted: old password reset flow credentials" );
	}

	@Test
	void should_return_all_blocks() {
		try (var session = CONTEXT.createSession()) {
			session.put( NotionBlock.class,
					List.of( paragraph(), sensitiveBlock(), toggleWithChildren(), imageBlock(), trashedBlock() ) );

			var result = session.createQuery(
					"SELECT b.id, b.type, b.pageId FROM NotionBlock b",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 5 );
		}
	}

	@Test
	void should_find_blocks_with_plain_text() {
		try (var session = CONTEXT.createSession()) {
			session.put( NotionBlock.class,
					List.of( paragraph(), sensitiveBlock(), toggleWithChildren(), imageBlock(), trashedBlock() ) );

			var result = session.createQuery(
					"SELECT b.id, b.plainText FROM NotionBlock b WHERE b.plainText IS NOT NULL",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			// image block has no plainText
			assertThat( result ).hasSize( 4 );
		}
	}

	@Test
	void should_find_potentially_sensitive_blocks() {
		try (var session = CONTEXT.createSession()) {
			session.put( NotionBlock.class,
					List.of( paragraph(), sensitiveBlock(), toggleWithChildren(), imageBlock(), trashedBlock() ) );

			var result = session.createQuery(
					"SELECT b.id, b.pageId, b.plainText FROM NotionBlock b WHERE b.plainText LIKE '%secret%' OR b.plainText LIKE '%password%' OR b.plainText LIKE '%API_KEY%'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_blocks_with_children() {
		try (var session = CONTEXT.createSession()) {
			session.put( NotionBlock.class,
					List.of( paragraph(), sensitiveBlock(), toggleWithChildren(), imageBlock(), trashedBlock() ) );

			var result = session.createQuery(
					"SELECT b.id, b.type FROM NotionBlock b WHERE b.hasChildren = true",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "type" ) ).isEqualTo( "toggle" );
		}
	}

	@Test
	void should_find_trashed_blocks() {
		try (var session = CONTEXT.createSession()) {
			session.put( NotionBlock.class,
					List.of( paragraph(), sensitiveBlock(), toggleWithChildren(), imageBlock(), trashedBlock() ) );

			var result = session.createQuery(
					"SELECT b.id, b.plainText FROM NotionBlock b WHERE b.inTrash = true",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "plainText" ) ).asString().contains( "credentials" );
		}
	}

	@Test
	void should_count_blocks_per_page() {
		try (var session = CONTEXT.createSession()) {
			session.put( NotionBlock.class,
					List.of( paragraph(), sensitiveBlock(), toggleWithChildren(), imageBlock(), trashedBlock() ) );

			var result = session.createQuery(
					"SELECT b.pageId, COUNT(*) AS block_count FROM NotionBlock b GROUP BY b.pageId",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}
}
