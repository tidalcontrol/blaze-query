/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.notion;

import com.fasterxml.jackson.databind.JsonNode;

import static com.blazebit.query.connector.notion.NotionJsonUtils.*;

/**
 * Represents a Notion database as returned by the search API.
 *
 * <p>Security and compliance relevance:
 * <ul>
 *   <li>Inventory all structured data stores in the workspace.</li>
 *   <li>Identify inline databases that may be embedded inside broadly shared pages.</li>
 *   <li>Audit ownership — who created each database and when it was last modified.</li>
 *   <li>Track archived or trashed databases for data-retention policy enforcement.</li>
 * </ul>
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class NotionDatabase {

	private final String id;
	private final String title;
	private final String createdTime;
	private final String lastEditedTime;
	private final String createdById;
	private final String lastEditedById;
	private final boolean archived;
	private final boolean inTrash;
	/**
	 * Parent type: {@code "workspace"}, {@code "page_id"}, or {@code "block_id"}.
	 */
	private final String parentType;
	/**
	 * UUID of the parent resource, or {@code null} when the parent is the workspace root.
	 */
	private final String parentId;
	private final String url;
	/** {@code true} when the database is rendered inline inside a page. */
	private final boolean inline;

	NotionDatabase(
			String id,
			String title,
			String createdTime,
			String lastEditedTime,
			String createdById,
			String lastEditedById,
			boolean archived,
			boolean inTrash,
			String parentType,
			String parentId,
			String url,
			boolean inline) {
		this.id = id;
		this.title = title;
		this.createdTime = createdTime;
		this.lastEditedTime = lastEditedTime;
		this.createdById = createdById;
		this.lastEditedById = lastEditedById;
		this.archived = archived;
		this.inTrash = inTrash;
		this.parentType = parentType;
		this.parentId = parentId;
		this.url = url;
		this.inline = inline;
	}

	/**
	 * Constructs a {@link NotionDatabase} from a raw Notion API JSON node.
	 */
	public static NotionDatabase fromJson(JsonNode node) {
		String id = text( node, "id" );
		String createdTime = text( node, "created_time" );
		String lastEditedTime = text( node, "last_edited_time" );
		String createdById = nestedId( node, "created_by" );
		String lastEditedById = nestedId( node, "last_edited_by" );
		boolean archived = booleanField( node, "archived" );
		boolean inTrash = booleanField( node, "in_trash" );
		boolean inline = booleanField( node, "is_inline" );
		String url = text( node, "url" );

		String title = richTextToPlain( node.get( "title" ) );

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
					case "block_id":
						parentId = text( parent, "block_id" );
						break;
					default:
						// "workspace" — parentId stays null
						break;
				}
			}
		}

		return new NotionDatabase( id, title, createdTime, lastEditedTime, createdById, lastEditedById,
				archived, inTrash, parentType, parentId, url, inline );
	}

	/** Notion database UUID. */
	public String getId() {
		return id;
	}

	/** Plain-text title of the database, or {@code null} if untitled. */
	public String getTitle() {
		return title;
	}

	/** ISO 8601 timestamp when the database was created. */
	public String getCreatedTime() {
		return createdTime;
	}

	/** ISO 8601 timestamp of the most recent edit. */
	public String getLastEditedTime() {
		return lastEditedTime;
	}

	/** UUID of the user who created the database. */
	public String getCreatedById() {
		return createdById;
	}

	/** UUID of the user who last edited the database. */
	public String getLastEditedById() {
		return lastEditedById;
	}

	/** Whether the database has been archived via the Notion UI or API. */
	public boolean isArchived() {
		return archived;
	}

	/** Whether the database currently resides in the workspace trash. */
	public boolean isInTrash() {
		return inTrash;
	}

	/**
	 * The type of the database's parent: {@code "workspace"}, {@code "page_id"},
	 * or {@code "block_id"}.
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

	/** Public Notion URL for this database. */
	public String getUrl() {
		return url;
	}

	/**
	 * Whether the database is rendered inline within a page rather than as a standalone
	 * full-page database. Inline databases are often less visible and may be overlooked
	 * during access reviews.
	 */
	public boolean isInline() {
		return inline;
	}
}
