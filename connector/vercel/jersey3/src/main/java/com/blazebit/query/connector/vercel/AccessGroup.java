/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.vercel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A Vercel access group — a named collection of team members with a shared project role.
 *
 * <p>Returned by {@code GET /v1/access-groups}.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccessGroup {

	private String accessGroupId;
	private String name;
	private String teamId;
	private Integer membersCount;
	private Integer projectsCount;
	private Boolean isDsyncManaged;
	private String createdAt;
	private String updatedAt;

	@JsonProperty("accessGroupId")
	public String getAccessGroupId() {
		return accessGroupId;
	}

	public void setAccessGroupId(String accessGroupId) {
		this.accessGroupId = accessGroupId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTeamId() {
		return teamId;
	}

	public void setTeamId(String teamId) {
		this.teamId = teamId;
	}

	public Integer getMembersCount() {
		return membersCount;
	}

	public void setMembersCount(Integer membersCount) {
		this.membersCount = membersCount;
	}

	public Integer getProjectsCount() {
		return projectsCount;
	}

	public void setProjectsCount(Integer projectsCount) {
		this.projectsCount = projectsCount;
	}

	/** Whether this access group is managed by a directory sync (SCIM) connection. */
	public Boolean getIsDsyncManaged() {
		return isDsyncManaged;
	}

	public void setIsDsyncManaged(Boolean isDsyncManaged) {
		this.isDsyncManaged = isDsyncManaged;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public String getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(String updatedAt) {
		this.updatedAt = updatedAt;
	}
}
