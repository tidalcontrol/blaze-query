/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.notion;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a Notion block — the atomic unit of page content.
 *
 * <p>Each {@link NotionPage} is composed of a tree of blocks. Only the first level of
 * children is stored per block; the {@link #isHasChildren()} flag indicates whether
 * deeper nesting exists.
 *
 * <p>Security and compliance relevance:
 * <ul>
 *   <li>DLP scanning — use {@link #getPlainText()} to inspect the textual content of
 *       pages for sensitive data (PII, credentials, etc.).</li>
 *   <li>Authorship audit — {@link #getCreatedById()} and {@link #getLastEditedById()}
 *       track who wrote or modified each block.</li>
 *   <li>Retention — {@link #isInTrash()} identifies soft-deleted blocks that are still
 *       recoverable and may contain sensitive content.</li>
 * </ul>
 *
 * <p><b>Performance note:</b> {@link NotionBlockDataFetcher} issues one API call per
 * accessible page to retrieve top-level blocks. In large workspaces this can be expensive;
 * consider filtering to specific pages using SQL {@code WHERE pageId = ?} predicates.
 *
 * @author Blazebit
 * @since 1.0.0
 */
public class NotionBlock {

	private final String id;
	/** UUID of the page that directly contains this block. */
	private final String pageId;
	private final String type;
	private final String createdTime;
	private final String lastEditedTime;
	private final String createdById;
	private final String lastEditedById;
	private final boolean hasChildren;
	private final boolean inTrash;
	/**
	 * Concatenated plain-text content extracted from the block's rich-text fields.
	 * Non-null for text-based block types; {@code null} for structural or media blocks.
	 */
	private final String plainText;

	/** Block types whose content is carried in a {@code rich_text} array. */
	private static final java.util.Set<String> TEXT_BLOCK_TYPES = java.util.Set.of(
			"paragraph", "heading_1", "heading_2", "heading_3", "heading_4",
			"bulleted_list_item", "numbered_list_item", "quote", "callout",
			"code", "toggle", "to_do" );

	private NotionBlock(
			String id,
			String pageId,
			String type,
			String createdTime,
			String lastEditedTime,
			String createdById,
			String lastEditedById,
			boolean hasChildren,
			boolean inTrash,
			String plainText) {
		this.id = id;
		this.pageId = pageId;
		this.type = type;
		this.createdTime = createdTime;
		this.lastEditedTime = lastEditedTime;
		this.createdById = createdById;
		this.lastEditedById = lastEditedById;
		this.hasChildren = hasChildren;
		this.inTrash = inTrash;
		this.plainText = plainText;
	}

	/**
	 * Constructs a {@link NotionBlock} from a raw Notion API JSON node.
	 *
	 * @param node the block JSON node
	 * @param pageId the UUID of the page this block belongs to
	 */
	public static NotionBlock fromJson(JsonNode node, String pageId) {
		String id = text( node, "id" );
		String type = text( node, "type" );
		String createdTime = text( node, "created_time" );
		String lastEditedTime = text( node, "last_edited_time" );
		String createdById = nestedId( node, "created_by" );
		String lastEditedById = nestedId( node, "last_edited_by" );
		boolean hasChildren = booleanField( node, "has_children" );
		boolean inTrash = booleanField( node, "in_trash" );
		String plainText = extractPlainText( node, type );

		return new NotionBlock( id, pageId, type, createdTime, lastEditedTime,
				createdById, lastEditedById, hasChildren, inTrash, plainText );
	}

	private static String extractPlainText(JsonNode node, String type) {
		if ( type == null || !TEXT_BLOCK_TYPES.contains( type ) ) {
			return null;
		}
		JsonNode typeNode = node.get( type );
		if ( typeNode == null ) {
			return null;
		}
		JsonNode richText = typeNode.get( "rich_text" );
		if ( richText == null || !richText.isArray() ) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		for ( JsonNode segment : richText ) {
			JsonNode pt = segment.get( "plain_text" );
			if ( pt != null && !pt.isNull() ) {
				sb.append( pt.asText() );
			}
		}
		return sb.length() > 0 ? sb.toString() : null;
	}

	private static String text(JsonNode node, String field) {
		JsonNode value = node.get( field );
		return ( value == null || value.isNull() ) ? null : value.asText();
	}

	private static String nestedId(JsonNode node, String field) {
		JsonNode nested = node.get( field );
		return ( nested == null ) ? null : text( nested, "id" );
	}

	private static boolean booleanField(JsonNode node, String field) {
		JsonNode value = node.get( field );
		return value != null && value.asBoolean();
	}

	/** Notion block UUID. */
	public String getId() {
		return id;
	}

	/** UUID of the page this block was fetched from. */
	public String getPageId() {
		return pageId;
	}

	/**
	 * Block type, e.g. {@code "paragraph"}, {@code "heading_1"}, {@code "image"},
	 * {@code "child_page"}, etc.
	 */
	public String getType() {
		return type;
	}

	/** ISO 8601 timestamp when the block was created. */
	public String getCreatedTime() {
		return createdTime;
	}

	/** ISO 8601 timestamp of the most recent edit. */
	public String getLastEditedTime() {
		return lastEditedTime;
	}

	/** UUID of the user who created this block. */
	public String getCreatedById() {
		return createdById;
	}

	/** UUID of the user who last edited this block. */
	public String getLastEditedById() {
		return lastEditedById;
	}

	/** Whether this block has nested child blocks. */
	public boolean isHasChildren() {
		return hasChildren;
	}

	/** Whether this block has been soft-deleted into the workspace trash. */
	public boolean isInTrash() {
		return inTrash;
	}

	/**
	 * Concatenated plain-text content of this block's rich-text fields.
	 * Available for paragraph, heading, list, quote, callout, code, toggle, and to-do
	 * block types. {@code null} for structural or media blocks.
	 */
	public String getPlainText() {
		return plainText;
	}
}
