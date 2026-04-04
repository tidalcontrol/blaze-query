/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.notion;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a Notion workspace user (person or bot integration).
 *
 * <p>This maps to the Notion {@code User} object returned by {@code GET /v1/users}.
 * Both {@code person} and {@code bot} user types are represented; fields irrelevant
 * to a given type are {@code null}.
 *
 * <p>Security and compliance relevance:
 * <ul>
 *   <li>Enumerate all principals (humans and integrations) with workspace access.</li>
 *   <li>Identify bot integrations and their workspace vs. user ownership.</li>
 *   <li>Correlate email addresses with your identity provider for access reviews.</li>
 * </ul>
 *
 * @author Blazebit
 * @since 1.0.0
 */
public class NotionUser {

	private final String id;
	private final String type;
	private final String name;
	private final String avatarUrl;
	/** Non-null only when {@code type = "person"}. */
	private final String email;
	/** Non-null only when {@code type = "bot"} and the bot is owned by a user. */
	private final String botOwnerId;
	/** {@code "workspace"} or {@code "user"} — non-null only when {@code type = "bot"}. */
	private final String botOwnerType;

	private NotionUser(
			String id,
			String type,
			String name,
			String avatarUrl,
			String email,
			String botOwnerId,
			String botOwnerType) {
		this.id = id;
		this.type = type;
		this.name = name;
		this.avatarUrl = avatarUrl;
		this.email = email;
		this.botOwnerId = botOwnerId;
		this.botOwnerType = botOwnerType;
	}

	/**
	 * Constructs a {@link NotionUser} from a raw Notion API JSON node.
	 */
	public static NotionUser fromJson(JsonNode node) {
		String id = text( node, "id" );
		String type = text( node, "type" );
		String name = text( node, "name" );
		String avatarUrl = text( node, "avatar_url" );
		String email = null;
		String botOwnerId = null;
		String botOwnerType = null;

		if ( "person".equals( type ) ) {
			JsonNode person = node.get( "person" );
			if ( person != null ) {
				email = text( person, "email" );
			}
		}
		else if ( "bot".equals( type ) ) {
			JsonNode bot = node.get( "bot" );
			if ( bot != null && bot.has( "owner" ) ) {
				JsonNode owner = bot.get( "owner" );
				botOwnerType = text( owner, "type" );
				if ( "user".equals( botOwnerType ) && owner.has( "user" ) ) {
					botOwnerId = text( owner.get( "user" ), "id" );
				}
			}
		}

		return new NotionUser( id, type, name, avatarUrl, email, botOwnerId, botOwnerType );
	}

	private static String text(JsonNode node, String field) {
		JsonNode value = node.get( field );
		return ( value == null || value.isNull() ) ? null : value.asText();
	}

	/** Notion user UUID. */
	public String getId() {
		return id;
	}

	/** {@code "person"} or {@code "bot"}. */
	public String getType() {
		return type;
	}

	/** Display name of the user or bot. */
	public String getName() {
		return name;
	}

	/** Avatar image URL, or {@code null} if not set. */
	public String getAvatarUrl() {
		return avatarUrl;
	}

	/** Primary email address — present only for {@code type = "person"}. */
	public String getEmail() {
		return email;
	}

	/**
	 * The Notion user ID of the user who created the bot integration.
	 * Present only when {@code type = "bot"} and {@link #getBotOwnerType()} is {@code "user"}.
	 */
	public String getBotOwnerId() {
		return botOwnerId;
	}

	/**
	 * Who owns the bot: {@code "workspace"} (a workspace-level integration) or
	 * {@code "user"} (a personal integration). Present only when {@code type = "bot"}.
	 */
	public String getBotOwnerType() {
		return botOwnerType;
	}
}
