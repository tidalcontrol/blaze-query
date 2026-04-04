/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.vercel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * A Vercel project environment variable.
 *
 * <p>Returned by {@code GET /v9/projects/{idOrName}/env}.
 *
 * <p>Security-relevant fields: {@code type} (plain-text vs encrypted vs sensitive),
 * {@code target} (whether a secret reaches production), and {@code system}
 * (whether this is an auto-exposed system variable).
 *
 * @author Blazebit
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentVariable {

	private String id;
	private String key;
	private String type;
	private List<String> target;
	private String gitBranch;
	private String projectId;
	private Boolean systemGenerated;
	private String comment;
	private Long createdAt;
	private Long updatedAt;
	private String createdBy;
	private String updatedBy;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * Storage type: {@code plain}, {@code encrypted}, {@code sensitive}, {@code secret},
	 * or {@code system}. Plain-text variables are the highest risk for secret exposure.
	 */
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	/**
	 * The deployment targets this variable is available in:
	 * {@code production}, {@code preview}, and/or {@code development}.
	 */
	public List<String> getTarget() {
		return target;
	}

	public void setTarget(List<String> target) {
		this.target = target;
	}

	/** Git branch this variable is scoped to, or {@code null} for all branches. */
	public String getGitBranch() {
		return gitBranch;
	}

	public void setGitBranch(String gitBranch) {
		this.gitBranch = gitBranch;
	}

	/** The project this variable belongs to (set by the fetcher). */
	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	/** Whether this is an auto-generated system variable (e.g. {@code VERCEL_URL}). */
	@JsonProperty("system")
	public Boolean getSystemGenerated() {
		return systemGenerated;
	}

	public void setSystemGenerated(Boolean systemGenerated) {
		this.systemGenerated = systemGenerated;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
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

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}
}
