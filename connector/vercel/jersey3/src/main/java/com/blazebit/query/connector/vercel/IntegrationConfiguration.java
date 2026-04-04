/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.vercel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * A Vercel integration configuration — an installed third-party integration.
 *
 * <p>Returned by {@code GET /v1/integrations/configurations}.
 *
 * <p>Security-relevant: {@code scopes} shows what the integration can access,
 * {@code status} shows whether it is active or suspended, and
 * {@code projectSelection} shows whether it has access to all projects.
 *
 * @author Blazebit
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IntegrationConfiguration {

	private String id;
	private String slug;
	private String integrationId;
	private String ownerId;
	private String teamId;
	private String userId;
	private String status;
	private String source;
	private String installationType;
	private List<String> scopes;
	private List<String> projects;
	private Long createdAt;
	private Long updatedAt;
	private Long disabledAt;
	private String disabledReason;
	private Long deletedAt;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	/** The integration's slug/name (e.g. {@code datadog}, {@code sentry}). */
	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public String getIntegrationId() {
		return integrationId;
	}

	public void setIntegrationId(String integrationId) {
		this.integrationId = integrationId;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public String getTeamId() {
		return teamId;
	}

	public void setTeamId(String teamId) {
		this.teamId = teamId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * Installation status: {@code ready}, {@code pending}, {@code error},
	 * {@code suspended}, {@code onboarding}, or {@code uninstalled}.
	 */
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	/** Install source: {@code marketplace}, {@code external}, {@code oauth}, {@code cli}, etc. */
	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getInstallationType() {
		return installationType;
	}

	public void setInstallationType(String installationType) {
		this.installationType = installationType;
	}

	/** OAuth scopes the integration has been granted. Broader scopes = higher risk. */
	public List<String> getScopes() {
		return scopes;
	}

	public void setScopes(List<String> scopes) {
		this.scopes = scopes;
	}

	/** Project IDs this integration has access to; empty means all projects. */
	public List<String> getProjects() {
		return projects;
	}

	public void setProjects(List<String> projects) {
		this.projects = projects;
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

	/** Timestamp when the integration was disabled, or {@code null} if active. */
	public Long getDisabledAt() {
		return disabledAt;
	}

	public void setDisabledAt(Long disabledAt) {
		this.disabledAt = disabledAt;
	}

	public String getDisabledReason() {
		return disabledReason;
	}

	public void setDisabledReason(String disabledReason) {
		this.disabledReason = disabledReason;
	}

	/** Timestamp when the integration was deleted, or {@code null} if still installed. */
	public Long getDeletedAt() {
		return deletedAt;
	}

	public void setDeletedAt(Long deletedAt) {
		this.deletedAt = deletedAt;
	}
}
