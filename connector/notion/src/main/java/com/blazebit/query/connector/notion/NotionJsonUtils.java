/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.notion;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Shared JSON extraction utilities for Notion API response parsing.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
final class NotionJsonUtils {

	private NotionJsonUtils() {
	}

	/**
	 * Returns the text value of a field, or {@code null} if the field is absent or JSON null.
	 */
	static String text(JsonNode node, String field) {
		JsonNode value = node.get( field );
		return ( value == null || value.isNull() ) ? null : value.asText();
	}

	/**
	 * Returns the {@code id} sub-field of a nested object, or {@code null} if absent.
	 */
	static String nestedId(JsonNode node, String field) {
		JsonNode nested = node.get( field );
		return ( nested == null ) ? null : text( nested, "id" );
	}

	/**
	 * Returns a boolean field value, defaulting to {@code false} if absent.
	 */
	static boolean booleanField(JsonNode node, String field) {
		JsonNode value = node.get( field );
		return value != null && value.asBoolean();
	}

	/**
	 * Concatenates {@code plain_text} values from a Notion rich-text JSON array.
	 *
	 * @param richTextArray a JSON array of rich-text segments
	 * @return the concatenated plain text, or {@code null} if the array is empty or not an array
	 */
	static String richTextToPlain(JsonNode richTextArray) {
		if ( richTextArray == null || !richTextArray.isArray() ) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		for ( JsonNode segment : richTextArray ) {
			JsonNode pt = segment.get( "plain_text" );
			if ( pt != null && !pt.isNull() ) {
				sb.append( pt.asText() );
			}
		}
		return !sb.isEmpty() ? sb.toString() : null;
	}
}
