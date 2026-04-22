/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.vercel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A Vercel deployment.
 *
 * <p>Returned by {@code GET /v6/deployments}.
 *
 * <p>Security-relevant: {@code target} (production vs preview), {@code source}
 * (detects non-git deploys like {@code cli} or {@code api-trigger}), {@code creator},
 * and {@code state}/{@code checksConclusion} for failed security checks.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Deployment {

	private String uid;
	private String name;
	private String projectId;
	private String url;
	private String state;
	private String target;
	private String source;
	private String checksConclusion;
	private String errorCode;
	private String errorMessage;
	private Creator creator;
	private Long created;
	private Long buildingAt;
	private Long ready;

	@JsonProperty("uid")
	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Deployment state: {@code BUILDING}, {@code READY}, {@code ERROR},
	 * {@code QUEUED}, {@code INITIALIZING}, {@code CANCELED}, or {@code DELETED}.
	 */
	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	/**
	 * Deployment target: {@code production}, {@code staging}, or {@code null}
	 * for preview deployments.
	 */
	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	/**
	 * How the deployment was triggered: {@code git}, {@code cli},
	 * {@code api-trigger-git-deploy}, {@code import}, {@code redeploy}, etc.
	 * Non-git sources may indicate out-of-band deployments.
	 */
	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	/**
	 * Deployment check conclusion: {@code succeeded}, {@code failed},
	 * {@code skipped}, or {@code canceled}.
	 */
	public String getChecksConclusion() {
		return checksConclusion;
	}

	public void setChecksConclusion(String checksConclusion) {
		this.checksConclusion = checksConclusion;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public Creator getCreator() {
		return creator;
	}

	public void setCreator(Creator creator) {
		this.creator = creator;
	}

	/** Unix timestamp (ms) when the deployment was created. */
	public Long getCreated() {
		return created;
	}

	public void setCreated(Long created) {
		this.created = created;
	}

	public Long getBuildingAt() {
		return buildingAt;
	}

	public void setBuildingAt(Long buildingAt) {
		this.buildingAt = buildingAt;
	}

	public Long getReady() {
		return ready;
	}

	public void setReady(Long ready) {
		this.ready = ready;
	}

	/**
	 * The user who triggered the deployment.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Creator {

		private String uid;
		private String email;
		private String username;

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

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}
	}
}
