/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.notion;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a row (page) within a Notion database.
 *
 * <p>Each row is a Notion page whose properties conform to the parent database schema.
 * Property values are extracted into two queryable forms:
 * <ul>
 *   <li>{@link #getPropertiesPlainText()} — all property values concatenated into a
 *       single string, suitable for DLP keyword/pattern scanning.</li>
 *   <li>{@link #getTitle()} — the value of the title property (the primary identifier
 *       of the row in the database).</li>
 * </ul>
 *
 * <p>Security and compliance relevance:
 * <ul>
 *   <li>Row-level DLP — databases are commonly used for HR records, vendor lists,
 *       customer contacts, and incident trackers, all of which may contain PII.</li>
 *   <li>Ownership audit — who created or last edited each row.</li>
 *   <li>Retention — rows in archived/trashed databases remain API-accessible until
 *       permanently deleted.</li>
 * </ul>
 *
 * <p>This fetcher is disabled by default. Enable via
 * {@link NotionConnectorConfig#DATABASE_ROWS_ENABLED}.
 *
 * @author Blazebit
 * @since 1.0.0
 */
public class NotionDatabaseRow {

	private final String id;
	private final String databaseId;
	private final String createdTime;
	private final String lastEditedTime;
	private final String createdById;
	private final String lastEditedById;
	private final boolean archived;
	private final boolean inTrash;
	private final String title;
	/**
	 * Concatenated plain-text representation of all property values in this row.
	 * Property names and values are joined as {@code "Name: value; "} pairs.
	 */
	private final String propertiesPlainText;

	private NotionDatabaseRow(
			String id,
			String databaseId,
			String createdTime,
			String lastEditedTime,
			String createdById,
			String lastEditedById,
			boolean archived,
			boolean inTrash,
			String title,
			String propertiesPlainText) {
		this.id = id;
		this.databaseId = databaseId;
		this.createdTime = createdTime;
		this.lastEditedTime = lastEditedTime;
		this.createdById = createdById;
		this.lastEditedById = lastEditedById;
		this.archived = archived;
		this.inTrash = inTrash;
		this.title = title;
		this.propertiesPlainText = propertiesPlainText;
	}

	/**
	 * Constructs a {@link NotionDatabaseRow} from a raw Notion API JSON node.
	 *
	 * @param node the page/row JSON node returned by the database query endpoint
	 * @param databaseId the UUID of the database this row belongs to
	 */
	public static NotionDatabaseRow fromJson(JsonNode node, String databaseId) {
		String id = text( node, "id" );
		String createdTime = text( node, "created_time" );
		String lastEditedTime = text( node, "last_edited_time" );
		String createdById = nestedId( node, "created_by" );
		String lastEditedById = nestedId( node, "last_edited_by" );
		boolean archived = booleanField( node, "archived" );
		boolean inTrash = booleanField( node, "in_trash" );

		String title = null;
		StringBuilder propertiesSb = new StringBuilder();

		JsonNode properties = node.get( "properties" );
		if ( properties != null ) {
			java.util.Iterator<java.util.Map.Entry<String, JsonNode>> fields = properties.fields();
			while ( fields.hasNext() ) {
				java.util.Map.Entry<String, JsonNode> entry = fields.next();
				String propName = entry.getKey();
				JsonNode propValue = entry.getValue();
				String extracted = extractPropertyText( propValue );
				if ( extracted != null ) {
					if ( propertiesSb.length() > 0 ) {
						propertiesSb.append( "; " );
					}
					propertiesSb.append( propName ).append( ": " ).append( extracted );
					// Use the title property as the row's primary identifier
					if ( "title".equals( text( propValue, "type" ) ) && title == null ) {
						title = extracted;
					}
				}
			}
		}

		return new NotionDatabaseRow( id, databaseId, createdTime, lastEditedTime,
				createdById, lastEditedById, archived, inTrash, title,
				propertiesSb.length() > 0 ? propertiesSb.toString() : null );
	}

	private static String extractPropertyText(JsonNode prop) {
		String type = text( prop, "type" );
		if ( type == null ) {
			return null;
		}
		JsonNode value = prop.get( type );
		if ( value == null || value.isNull() ) {
			return null;
		}
		switch ( type ) {
			case "title":
			case "rich_text":
				return richTextToPlain( value );
			case "email":
			case "phone_number":
			case "url":
			case "number":
				return value.asText( null );
			case "checkbox":
				return value.asBoolean() ? "true" : "false";
			case "select":
				return text( value, "name" );
			case "multi_select":
				if ( value.isArray() ) {
					StringBuilder sb = new StringBuilder();
					for ( JsonNode opt : value ) {
						if ( sb.length() > 0 ) sb.append( ", " );
						String name = text( opt, "name" );
						if ( name != null ) sb.append( name );
					}
					return sb.length() > 0 ? sb.toString() : null;
				}
				return null;
			case "date":
				String start = text( value, "start" );
				String end = text( value, "end" );
				return end != null ? start + " → " + end : start;
			case "people":
				if ( value.isArray() ) {
					StringBuilder sb = new StringBuilder();
					for ( JsonNode person : value ) {
						if ( sb.length() > 0 ) sb.append( ", " );
						String name = text( person, "name" );
						if ( name != null ) sb.append( name );
					}
					return sb.length() > 0 ? sb.toString() : null;
				}
				return null;
			case "status":
				return text( value, "name" );
			default:
				return null;
		}
	}

	private static String richTextToPlain(JsonNode richTextArray) {
		if ( !richTextArray.isArray() ) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		for ( JsonNode segment : richTextArray ) {
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

	/** Notion row UUID (same as the underlying page UUID). */
	public String getId() {
		return id;
	}

	/** UUID of the database this row belongs to. */
	public String getDatabaseId() {
		return databaseId;
	}

	/** ISO 8601 timestamp when the row was created. */
	public String getCreatedTime() {
		return createdTime;
	}

	/** ISO 8601 timestamp of the most recent edit. */
	public String getLastEditedTime() {
		return lastEditedTime;
	}

	/** UUID of the user who created this row. */
	public String getCreatedById() {
		return createdById;
	}

	/** UUID of the user who last edited this row. */
	public String getLastEditedById() {
		return lastEditedById;
	}

	/** Whether the row has been archived. */
	public boolean isArchived() {
		return archived;
	}

	/** Whether the row is currently in the workspace trash. */
	public boolean isInTrash() {
		return inTrash;
	}

	/**
	 * Value of the database's title property — the primary human-readable identifier
	 * for this row. {@code null} if the title property is empty.
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * All property values concatenated as {@code "Property: value; "} pairs.
	 * Covers title, rich_text, email, phone_number, url, number, checkbox, select,
	 * multi_select, date, people, and status property types.
	 * Suitable for DLP keyword and regex scanning across an entire row.
	 */
	public String getPropertiesPlainText() {
		return propertiesPlainText;
	}
}
