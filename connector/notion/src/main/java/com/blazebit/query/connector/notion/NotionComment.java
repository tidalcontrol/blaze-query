/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.notion;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a Notion comment on a page or block.
 *
 * <p>Security and compliance relevance:
 * <ul>
 *   <li>DLP — comments are a common location for credential leaks, PII, and internal
 *       URLs that bypass page-level content policies.</li>
 *   <li>Audit trail — author and timestamp are preserved even after the page content
 *       around the comment is edited or deleted.</li>
 *   <li>Retention — comments on trashed pages are still accessible via the API until
 *       the page is permanently deleted.</li>
 * </ul>
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class NotionComment {

	private final String id;
	/** UUID of the page that contains this comment (or the page owning the block). */
	private final String pageId;
	/**
	 * UUID of the specific block this comment is anchored to, or {@code null} when the
	 * comment is a top-level page comment.
	 */
	private final String blockId;
	private final String discussionId;
	private final String createdTime;
	private final String lastEditedTime;
	private final String createdById;
	/** Concatenated plain-text content of the comment's rich-text segments. */
	private final String plainText;

	NotionComment(
			String id,
			String pageId,
			String blockId,
			String discussionId,
			String createdTime,
			String lastEditedTime,
			String createdById,
			String plainText) {
		this.id = id;
		this.pageId = pageId;
		this.blockId = blockId;
		this.discussionId = discussionId;
		this.createdTime = createdTime;
		this.lastEditedTime = lastEditedTime;
		this.createdById = createdById;
		this.plainText = plainText;
	}

	/**
	 * Constructs a {@link NotionComment} from a raw Notion API JSON node.
	 *
	 * @param node the comment JSON node
	 * @param pageId the UUID of the page this comment was fetched from
	 */
	public static NotionComment fromJson(JsonNode node, String pageId) {
		String id = text( node, "id" );
		String discussionId = text( node, "discussion_id" );
		String createdTime = text( node, "created_time" );
		String lastEditedTime = text( node, "last_edited_time" );
		String createdById = nestedId( node, "created_by" );

		// Determine whether this is a block-level or page-level comment from the parent field
		String blockId = null;
		JsonNode parent = node.get( "parent" );
		if ( parent != null ) {
			String parentType = text( parent, "type" );
			if ( "block_id".equals( parentType ) ) {
				blockId = text( parent, "block_id" );
			}
		}

		// Extract concatenated plain text from the rich_text array
		String plainText = null;
		JsonNode richText = node.get( "rich_text" );
		if ( richText != null && richText.isArray() ) {
			StringBuilder sb = new StringBuilder();
			for ( JsonNode segment : richText ) {
				JsonNode pt = segment.get( "plain_text" );
				if ( pt != null && !pt.isNull() ) {
					sb.append( pt.asText() );
				}
			}
			if ( sb.length() > 0 ) {
				plainText = sb.toString();
			}
		}

		return new NotionComment( id, pageId, blockId, discussionId, createdTime, lastEditedTime,
				createdById, plainText );
	}

	private static String text(JsonNode node, String field) {
		JsonNode value = node.get( field );
		return ( value == null || value.isNull() ) ? null : value.asText();
	}

	private static String nestedId(JsonNode node, String field) {
		JsonNode nested = node.get( field );
		return ( nested == null ) ? null : text( nested, "id" );
	}

	/** Notion comment UUID. */
	public String getId() {
		return id;
	}

	/** UUID of the page that contains this comment. */
	public String getPageId() {
		return pageId;
	}

	/**
	 * UUID of the block this comment is anchored to, or {@code null} for top-level
	 * page comments.
	 */
	public String getBlockId() {
		return blockId;
	}

	/** UUID of the discussion thread this comment belongs to. */
	public String getDiscussionId() {
		return discussionId;
	}

	/** ISO 8601 timestamp when the comment was created. */
	public String getCreatedTime() {
		return createdTime;
	}

	/** ISO 8601 timestamp of the most recent edit to this comment. */
	public String getLastEditedTime() {
		return lastEditedTime;
	}

	/** UUID of the user who wrote this comment. */
	public String getCreatedById() {
		return createdById;
	}

	/**
	 * Concatenated plain-text content of the comment, suitable for DLP pattern matching.
	 */
	public String getPlainText() {
		return plainText;
	}
}
