/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.notion;

import com.fasterxml.jackson.databind.JsonNode;

import static com.blazebit.query.connector.notion.NotionJsonUtils.*;

/**
 * Represents a Notion page as returned by the search API.
 *
 * <p>Security and compliance relevance:
 * <ul>
 *   <li>Audit content ownership — who created and last edited each page.</li>
 *   <li>Identify locked pages ({@link #isLocked()}) to track content freeze controls.</li>
 *   <li>Identify publicly published pages ({@link #getPublicUrl()}) for data-exposure reviews.</li>
 *   <li>Identify pages in trash or archived state for retention policy enforcement.</li>
 *   <li>Map the page hierarchy (workspace → database → page) to understand data exposure.</li>
 *   <li>Detect orphaned or top-level workspace pages that may be broadly accessible.</li>
 * </ul>
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class NotionPage {

	private final String id;
	private final String createdTime;
	private final String lastEditedTime;
	private final String createdById;
	private final String lastEditedById;
	private final boolean archived;
	private final boolean inTrash;
	private final boolean locked;
	/**
	 * Non-null when the page has been published to the web via Notion's
	 * "Share to web" feature — indicates the page is publicly accessible.
	 */
	private final String publicUrl;
	/**
	 * Parent type: {@code "workspace"}, {@code "page_id"}, {@code "database_id"},
	 * or {@code "block_id"}.
	 */
	private final String parentType;
	/**
	 * UUID of the parent resource, or {@code null} when the parent type is
	 * {@code "workspace"}.
	 */
	private final String parentId;
	private final String url;

	NotionPage(
			String id,
			String createdTime,
			String lastEditedTime,
			String createdById,
			String lastEditedById,
			boolean archived,
			boolean inTrash,
			boolean locked,
			String publicUrl,
			String parentType,
			String parentId,
			String url) {
		this.id = id;
		this.createdTime = createdTime;
		this.lastEditedTime = lastEditedTime;
		this.createdById = createdById;
		this.lastEditedById = lastEditedById;
		this.archived = archived;
		this.inTrash = inTrash;
		this.locked = locked;
		this.publicUrl = publicUrl;
		this.parentType = parentType;
		this.parentId = parentId;
		this.url = url;
	}

	/**
	 * Constructs a {@link NotionPage} from a raw Notion API JSON node.
	 */
	public static NotionPage fromJson(JsonNode node) {
		String id = text( node, "id" );
		String createdTime = text( node, "created_time" );
		String lastEditedTime = text( node, "last_edited_time" );
		String createdById = nestedId( node, "created_by" );
		String lastEditedById = nestedId( node, "last_edited_by" );
		boolean archived = booleanField( node, "archived" );
		boolean inTrash = booleanField( node, "in_trash" );
		boolean locked = booleanField( node, "is_locked" );
		String publicUrl = text( node, "public_url" );

		String parentType = null;
		String parentId = null;
		JsonNode parent = node.get( "parent" );
		if ( parent != null ) {
			parentType = text( parent, "type" );
			if ( parentType != null ) {
				switch ( parentType ) {
					case "page_id":
						parentId = text( parent, "page_id" );
						break;
					case "database_id":
						parentId = text( parent, "database_id" );
						break;
					case "block_id":
						parentId = text( parent, "block_id" );
						break;
					default:
						// "workspace" — parentId stays null
						break;
				}
			}
		}

		String url = text( node, "url" );
		return new NotionPage( id, createdTime, lastEditedTime, createdById, lastEditedById,
				archived, inTrash, locked, publicUrl, parentType, parentId, url );
	}

	/** Notion page UUID. */
	public String getId() {
		return id;
	}

	/** ISO 8601 timestamp when the page was created. */
	public String getCreatedTime() {
		return createdTime;
	}

	/** ISO 8601 timestamp of the most recent edit. */
	public String getLastEditedTime() {
		return lastEditedTime;
	}

	/** UUID of the user who created the page. */
	public String getCreatedById() {
		return createdById;
	}

	/** UUID of the user who last edited the page. */
	public String getLastEditedById() {
		return lastEditedById;
	}

	/** Whether the page has been archived via the Notion UI or API. */
	public boolean isArchived() {
		return archived;
	}

	/** Whether the page currently resides in the workspace trash. */
	public boolean isInTrash() {
		return inTrash;
	}

	/**
	 * Whether the page is locked. Locked pages cannot be edited from the Notion UI,
	 * providing a lightweight content-freeze control.
	 */
	public boolean isLocked() {
		return locked;
	}

	/**
	 * The publicly accessible URL of this page if "Share to web" is enabled,
	 * or {@code null} if the page is not publicly published. A non-null value
	 * means the page content is visible to anyone on the internet.
	 */
	public String getPublicUrl() {
		return publicUrl;
	}

	/**
	 * The type of the page's parent: {@code "workspace"}, {@code "page_id"},
	 * {@code "database_id"}, or {@code "block_id"}.
	 */
	public String getParentType() {
		return parentType;
	}

	/**
	 * UUID of the parent resource, or {@code null} when the parent is the workspace root.
	 */
	public String getParentId() {
		return parentId;
	}

	/** Internal Notion URL for this page (requires login). */
	public String getUrl() {
		return url;
	}
}
