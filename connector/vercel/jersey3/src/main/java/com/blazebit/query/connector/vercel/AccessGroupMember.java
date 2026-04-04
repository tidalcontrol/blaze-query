/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.vercel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A member of a Vercel access group.
 *
 * <p>Returned by {@code GET /v1/access-groups/{idOrName}/members}.
 *
 * <p>Useful for access reviews: verifying who belongs to each group,
 * what role they hold, and their broader team role context.
 *
 * @author Blazebit
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccessGroupMember {

	private String uid;
	private String email;
	private String name;
	private String username;
	private String role;
	private String teamRole;
	private String accessGroupId;
	private Long createdAt;

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
	 * The member's role within the access group: {@code ADMIN},
	 * {@code PROJECT_DEVELOPER}, or {@code PROJECT_VIEWER}.
	 */
	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	/** The member's team-level role, for comparison with the access group role. */
	public String getTeamRole() {
		return teamRole;
	}

	public void setTeamRole(String teamRole) {
		this.teamRole = teamRole;
	}

	/** The access group ID this member belongs to (set by the fetcher). */
	public String getAccessGroupId() {
		return accessGroupId;
	}

	public void setAccessGroupId(String accessGroupId) {
		this.accessGroupId = accessGroupId;
	}

	public Long getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Long createdAt) {
		this.createdAt = createdAt;
	}
}
