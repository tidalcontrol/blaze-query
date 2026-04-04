/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.vercel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A member with a project-level role override.
 *
 * <p>Returned by {@code GET /v1/projects/{idOrName}/members}.
 *
 * <p>Security-relevant: project-level roles can be higher than a member's
 * team role. A team {@code VIEWER} can be a project {@code ADMIN}.
 *
 * @author Blazebit
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectMember {

	private String uid;
	private String email;
	private String name;
	private String username;
	private String role;
	private String teamRole;
	private String projectId;
	private Long createdAt;
	private Long updatedAt;

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
	 * The member's role on this specific project: {@code ADMIN}, {@code PROJECT_DEVELOPER},
	 * or {@code PROJECT_VIEWER}. May be higher than their team-level role.
	 */
	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	/** The member's team-level role for comparison with the project role. */
	public String getTeamRole() {
		return teamRole;
	}

	public void setTeamRole(String teamRole) {
		this.teamRole = teamRole;
	}

	/** The project ID this member belongs to (set by the fetcher). */
	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
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
}
