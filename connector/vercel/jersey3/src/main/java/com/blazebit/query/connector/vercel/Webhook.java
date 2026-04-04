/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.vercel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * A Vercel webhook — an HTTP endpoint that receives event notifications.
 *
 * <p>Returned by {@code GET /v1/webhooks}.
 *
 * <p>Security-relevant events include {@code firewall.attack},
 * {@code project.env-variable.created}, {@code integration-configuration.permission-upgraded},
 * and {@code deployment.checks.failed}.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Webhook {

	private String id;
	private String url;
	private List<String> events;
	private String ownerId;
	private String teamId;
	private List<String> projectIds;
	private Long createdAt;
	private Long updatedAt;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	/** The HTTPS URL that receives webhook POST requests. */
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * The list of event types this webhook subscribes to.
	 * Security-relevant values include {@code firewall.attack},
	 * {@code firewall.system-rule-anomaly}, {@code firewall.custom-rule-anomaly},
	 * {@code project.env-variable.created/updated/deleted},
	 * {@code integration-configuration.permission-upgraded}, and
	 * {@code deployment.checks.failed}.
	 */
	public List<String> getEvents() {
		return events;
	}

	public void setEvents(List<String> events) {
		this.events = events;
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

	/** Project IDs this webhook is scoped to; empty means team-wide. */
	public List<String> getProjectIds() {
		return projectIds;
	}

	public void setProjectIds(List<String> projectIds) {
		this.projectIds = projectIds;
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
