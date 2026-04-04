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

public class NotionCommentTests {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new NotionSchemaProvider() );
		builder.registerSchemaObjectAlias( NotionComment.class, "NotionComment" );
		CONTEXT = builder.build();
	}

	private static NotionComment pageComment() {
		return new NotionComment(
				"comment-001", "page-001", null, "discussion-001",
				"2024-03-10T09:00:00.000Z", "2024-03-10T09:00:00.000Z",
				"user-001", "Can we share this externally?" );
	}

	private static NotionComment blockComment() {
		return new NotionComment(
				"comment-002", "page-001", "block-005", "discussion-002",
				"2024-03-11T14:00:00.000Z", "2024-03-11T14:00:00.000Z",
				"user-002", "The access_token here is sk-prod-xyz789 — rotate after review" );
	}

	private static NotionComment anotherPageComment() {
		return new NotionComment(
				"comment-003", "page-002", null, "discussion-003",
				"2024-02-01T08:00:00.000Z", "2024-02-01T08:00:00.000Z",
				"user-003", "LGTM" );
	}

	@Test
	void should_return_all_comments() {
		try (var session = CONTEXT.createSession()) {
			session.put( NotionComment.class, List.of( pageComment(), blockComment(), anotherPageComment() ) );

			var result = session.createQuery(
					"SELECT c.id, c.pageId, c.createdById FROM NotionComment c",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}

	@Test
	void should_find_block_anchored_comments() {
		try (var session = CONTEXT.createSession()) {
			session.put( NotionComment.class, List.of( pageComment(), blockComment(), anotherPageComment() ) );

			var result = session.createQuery(
					"SELECT c.id, c.blockId FROM NotionComment c WHERE c.blockId IS NOT NULL",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "blockId" ) ).isEqualTo( "block-005" );
		}
	}

	@Test
	void should_find_sensitive_comment_content() {
		try (var session = CONTEXT.createSession()) {
			session.put( NotionComment.class, List.of( pageComment(), blockComment(), anotherPageComment() ) );

			var result = session.createQuery(
					"SELECT c.id, c.pageId, c.plainText FROM NotionComment c WHERE c.plainText LIKE '%token%' OR c.plainText LIKE '%secret%' OR c.plainText LIKE '%sk-%'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "id" ) ).isEqualTo( "comment-002" );
		}
	}

	@Test
	void should_count_comments_per_page() {
		try (var session = CONTEXT.createSession()) {
			session.put( NotionComment.class, List.of( pageComment(), blockComment(), anotherPageComment() ) );

			var result = session.createQuery(
					"SELECT c.pageId, COUNT(*) AS comment_count FROM NotionComment c GROUP BY c.pageId",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_filter_comments_by_author() {
		try (var session = CONTEXT.createSession()) {
			session.put( NotionComment.class, List.of( pageComment(), blockComment(), anotherPageComment() ) );

			var result = session.createQuery(
					"SELECT c.id, c.plainText FROM NotionComment c WHERE c.createdById = 'user-001'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
		}
	}
}
