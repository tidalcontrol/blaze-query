/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.notion;

import com.fasterxml.jackson.databind.JsonNode;

import static com.blazebit.query.connector.notion.NotionJsonUtils.text;

/**
 * Represents the Notion workspace that the integration bot belongs to,
 * as returned by {@code GET /v1/users/me}.
 *
 * <p>Security and compliance relevance:
 * <ul>
 *   <li>Confirm which workspace an integration token is scoped to — useful for
 *       multi-workspace environments to prevent token mix-ups.</li>
 *   <li>Identify the bot user ID for filtering bot activity out of human-authored
 *       content in audit queries.</li>
 *   <li>Inspect workspace-level resource limits (e.g. max file upload size) that
 *       may constrain data exfiltration via the API.</li>
 * </ul>
 *
 * <p><b>Note:</b> The Notion public API does not expose workspace security settings
 * (SAML SSO configuration, domain verification, SCIM provisioning). Those are available
 * only through the Notion Enterprise admin UI or the Security &amp; Compliance partner
 * programme's Discovery API.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class NotionWorkspace {

	private final String workspaceId;
	private final String workspaceName;
	private final String botId;
	private final String botName;
	private final String botAvatarUrl;
	/** Raw string representation of the max file upload size limit, e.g. {@code "5mb"}. */
	private final String maxFileUploadSize;

	NotionWorkspace(
			String workspaceId,
			String workspaceName,
			String botId,
			String botName,
			String botAvatarUrl,
			String maxFileUploadSize) {
		this.workspaceId = workspaceId;
		this.workspaceName = workspaceName;
		this.botId = botId;
		this.botName = botName;
		this.botAvatarUrl = botAvatarUrl;
		this.maxFileUploadSize = maxFileUploadSize;
	}

	/**
	 * Constructs a {@link NotionWorkspace} from the raw {@code /v1/users/me} JSON node.
	 */
	public static NotionWorkspace fromJson(JsonNode node) {
		String botId = text( node, "id" );
		String botName = text( node, "name" );
		String botAvatarUrl = text( node, "avatar_url" );
		String workspaceId = null;
		String workspaceName = null;
		String maxFileUploadSize = null;

		JsonNode bot = node.get( "bot" );
		if ( bot != null ) {
			workspaceId = text( bot, "workspace_id" );
			workspaceName = text( bot, "workspace_name" );
			JsonNode limits = bot.get( "workspace_limits" );
			if ( limits != null ) {
				maxFileUploadSize = text( limits, "max_file_upload_size" );
			}
		}

		return new NotionWorkspace( workspaceId, workspaceName, botId, botName, botAvatarUrl,
				maxFileUploadSize );
	}

	/** The UUID of the Notion workspace this integration is installed in. */
	public String getWorkspaceId() {
		return workspaceId;
	}

	/** The display name of the workspace. */
	public String getWorkspaceName() {
		return workspaceName;
	}

	/** The UUID of the bot user that represents this integration token. */
	public String getBotId() {
		return botId;
	}

	/** The display name of the integration bot. */
	public String getBotName() {
		return botName;
	}

	/** Avatar image URL of the integration bot, or {@code null} if not set. */
	public String getBotAvatarUrl() {
		return botAvatarUrl;
	}

	/**
	 * The maximum file upload size permitted in this workspace (e.g. {@code "5mb"}),
	 * or {@code null} if not reported by the API.
	 */
	public String getMaxFileUploadSize() {
		return maxFileUploadSize;
	}
}
