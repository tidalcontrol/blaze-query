/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.vercel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * A Vercel authentication token.
 *
 * <p>Returned by {@code GET /v6/user/tokens}.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthToken {

	private String id;
	private String name;
	private String type;
	private String origin;
	private List<TokenScope> scopes;
	private Long expiresAt;
	private Long activeAt;
	private Long createdAt;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/** Token type: {@code oauth2-token} or {@code authentication-token}. */
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	/** Where the token originated: {@code github}, {@code gitlab}, {@code saml}, {@code email}, {@code manual}, etc. */
	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	public List<TokenScope> getScopes() {
		return scopes;
	}

	public void setScopes(List<TokenScope> scopes) {
		this.scopes = scopes;
	}

	/** Unix timestamp (ms) when the token expires, or {@code null} if it never expires. */
	public Long getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Long expiresAt) {
		this.expiresAt = expiresAt;
	}

	/** Unix timestamp (ms) of the most recent use of this token. */
	public Long getActiveAt() {
		return activeAt;
	}

	public void setActiveAt(Long activeAt) {
		this.activeAt = activeAt;
	}

	public Long getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Long createdAt) {
		this.createdAt = createdAt;
	}

	/**
	 * The scope of an auth token — either a user or a specific team.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class TokenScope {

		private String type;
		private String teamId;
		private String origin;
		private Long createdAt;
		private Long expiresAt;

		/** Scope type: {@code user} or {@code team}. */
		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getTeamId() {
			return teamId;
		}

		public void setTeamId(String teamId) {
			this.teamId = teamId;
		}

		public String getOrigin() {
			return origin;
		}

		public void setOrigin(String origin) {
			this.origin = origin;
		}

		public Long getCreatedAt() {
			return createdAt;
		}

		public void setCreatedAt(Long createdAt) {
			this.createdAt = createdAt;
		}

		public Long getExpiresAt() {
			return expiresAt;
		}

		public void setExpiresAt(Long expiresAt) {
			this.expiresAt = expiresAt;
		}
	}
}
