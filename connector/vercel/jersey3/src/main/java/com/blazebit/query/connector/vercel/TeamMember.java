/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.vercel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A member of a Vercel team.
 *
 * <p>Returned by {@code GET /v3/teams/{teamId}/members}.
 *
 * @author Blazebit
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TeamMember {

	private String uid;
	private String email;
	private String name;
	private String username;
	private String role;
	private String teamId;
	private Boolean confirmed;
	private Long createdAt;
	private Long updatedAt;
	private JoinedFrom joinedFrom;
	private Long accessRequestedAt;

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * The member's role in the team: {@code OWNER}, {@code MEMBER}, {@code DEVELOPER},
	 * {@code SECURITY}, {@code BILLING}, {@code VIEWER}, or {@code CONTRIBUTOR}.
	 */
	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	/** The ID of the team this member belongs to (set by the fetcher). */
	public String getTeamId() {
		return teamId;
	}

	public void setTeamId(String teamId) {
		this.teamId = teamId;
	}

	/** Whether the member has confirmed their email address and accepted the invite. */
	public Boolean getConfirmed() {
		return confirmed;
	}

	public void setConfirmed(Boolean confirmed) {
		this.confirmed = confirmed;
	}

	public Long getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Long createdAt) {
		this.createdAt = createdAt;
	}

	public Long getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Long updatedAt) {
		this.updatedAt = updatedAt;
	}

	/** How and where this member joined the team. */
	public JoinedFrom getJoinedFrom() {
		return joinedFrom;
	}

	public void setJoinedFrom(JoinedFrom joinedFrom) {
		this.joinedFrom = joinedFrom;
	}

	/** Unix timestamp (ms) when this member requested access (if pending). */
	public Long getAccessRequestedAt() {
		return accessRequestedAt;
	}

	public void setAccessRequestedAt(Long accessRequestedAt) {
		this.accessRequestedAt = accessRequestedAt;
	}

	/**
	 * Metadata about how a member joined the team.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class JoinedFrom {

		private String origin;
		private String dsyncTeamId;
		private String dsyncUserId;
		private Long dsyncConnectedAt;
		private Long samlConnectedAt;

		/**
		 * How the member joined: {@code dsync}, {@code saml}, {@code mail}, {@code import},
		 * {@code link}, {@code teams}, or {@code github}.
		 */
		public String getOrigin() {
			return origin;
		}

		public void setOrigin(String origin) {
			this.origin = origin;
		}

		/** Directory sync team ID if provisioned via SCIM. */
		public String getDsyncTeamId() {
			return dsyncTeamId;
		}

		public void setDsyncTeamId(String dsyncTeamId) {
			this.dsyncTeamId = dsyncTeamId;
		}

		/** Directory sync user ID if provisioned via SCIM. */
		public String getDsyncUserId() {
			return dsyncUserId;
		}

		public void setDsyncUserId(String dsyncUserId) {
			this.dsyncUserId = dsyncUserId;
		}

		/** Unix timestamp (ms) when this member was connected via directory sync. */
		public Long getDsyncConnectedAt() {
			return dsyncConnectedAt;
		}

		public void setDsyncConnectedAt(Long dsyncConnectedAt) {
			this.dsyncConnectedAt = dsyncConnectedAt;
		}

		/** Unix timestamp (ms) when this member authenticated via SAML. */
		public Long getSamlConnectedAt() {
			return samlConnectedAt;
		}

		public void setSamlConnectedAt(Long samlConnectedAt) {
			this.samlConnectedAt = samlConnectedAt;
		}
	}
}
