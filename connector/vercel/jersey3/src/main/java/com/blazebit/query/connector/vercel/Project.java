/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.vercel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A Vercel project with its security and deployment protection settings.
 *
 * <p>Returned by {@code GET /v9/projects}.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Project {

	private String id;
	private String name;
	private String accountId;
	private String framework;
	private ProtectionConfig passwordProtection;
	private SsoProtectionConfig ssoProtection;
	private Boolean autoExposeSystemEnvs;
	private Boolean enableAffectedProjectsDeployments;
	private Long createdAt;
	private Long updatedAt;

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

	/** The team or user account that owns this project. */
	public String getAccountId() {
		return accountId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public String getFramework() {
		return framework;
	}

	public void setFramework(String framework) {
		this.framework = framework;
	}

	/**
	 * Password protection configuration. Non-null means password protection is active.
	 * Relevant for assessing whether preview/production deployments require a password.
	 */
	public ProtectionConfig getPasswordProtection() {
		return passwordProtection;
	}

	public void setPasswordProtection(ProtectionConfig passwordProtection) {
		this.passwordProtection = passwordProtection;
	}

	/**
	 * SSO protection configuration. Non-null means SSO authentication is required.
	 * Only team members can access deployments when this is active.
	 */
	public SsoProtectionConfig getSsoProtection() {
		return ssoProtection;
	}

	public void setSsoProtection(SsoProtectionConfig ssoProtection) {
		this.ssoProtection = ssoProtection;
	}

	/**
	 * Whether system environment variables (e.g. {@code VERCEL_URL}) are automatically
	 * exposed to the project. Setting this to {@code false} reduces information exposure.
	 */
	public Boolean getAutoExposeSystemEnvs() {
		return autoExposeSystemEnvs;
	}

	public void setAutoExposeSystemEnvs(Boolean autoExposeSystemEnvs) {
		this.autoExposeSystemEnvs = autoExposeSystemEnvs;
	}

	public Boolean getEnableAffectedProjectsDeployments() {
		return enableAffectedProjectsDeployments;
	}

	public void setEnableAffectedProjectsDeployments(Boolean enableAffectedProjectsDeployments) {
		this.enableAffectedProjectsDeployments = enableAffectedProjectsDeployments;
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

	/**
	 * Password-based deployment protection settings.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ProtectionConfig {

		private String deploymentType;

		/**
		 * Which deployments are protected: {@code all_deployments},
		 * {@code only_preview_deployments}, or {@code only_production_deployments}.
		 */
		public String getDeploymentType() {
			return deploymentType;
		}

		public void setDeploymentType(String deploymentType) {
			this.deploymentType = deploymentType;
		}
	}

	/**
	 * SSO-based deployment protection settings.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class SsoProtectionConfig {

		private String deploymentType;

		/**
		 * Which deployments require SSO: {@code all_deployments},
		 * {@code only_preview_deployments}, or {@code only_production_deployments}.
		 */
		public String getDeploymentType() {
			return deploymentType;
		}

		public void setDeploymentType(String deploymentType) {
			this.deploymentType = deploymentType;
		}
	}
}
