/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.vercel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * A Vercel log drain — an HTTP endpoint that receives log events.
 *
 * <p>Returned by {@code GET /v1/log-drains}.
 *
 * <p>Security-relevant: missing log drains mean no audit trail; log drains
 * without {@code request} in their sources miss HTTP access logs.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LogDrain {

	private String id;
	private String name;
	private String url;
	private String deliveryFormat;
	private List<String> sources;
	private List<String> environments;
	private List<String> projectIds;
	private String teamId;
	private String clientId;
	private String configurationId;
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

	/** The HTTPS endpoint that receives log events. */
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	/** Log delivery format: {@code json}, {@code ndjson}, or {@code syslog}. */
	public String getDeliveryFormat() {
		return deliveryFormat;
	}

	public void setDeliveryFormat(String deliveryFormat) {
		this.deliveryFormat = deliveryFormat;
	}

	/**
	 * Log sources included: {@code build}, {@code edge}, {@code lambda},
	 * {@code request}, {@code external}, {@code firewall}.
	 * {@code request} is required to capture HTTP access logs.
	 */
	public List<String> getSources() {
		return sources;
	}

	public void setSources(List<String> sources) {
		this.sources = sources;
	}

	/** Deployment environments this drain applies to: {@code production}, {@code preview}. */
	public List<String> getEnvironments() {
		return environments;
	}

	public void setEnvironments(List<String> environments) {
		this.environments = environments;
	}

	/** Project IDs this drain is scoped to; empty means team-wide. */
	public List<String> getProjectIds() {
		return projectIds;
	}

	public void setProjectIds(List<String> projectIds) {
		this.projectIds = projectIds;
	}

	public String getTeamId() {
		return teamId;
	}

	public void setTeamId(String teamId) {
		this.teamId = teamId;
	}

	/** OAuth client ID if this drain was installed via an integration. */
	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getConfigurationId() {
		return configurationId;
	}

	public void setConfigurationId(String configurationId) {
		this.configurationId = configurationId;
	}

	public Long getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Long createdAt) {
		this.createdAt = createdAt;
	}
}
