/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.vercel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A Vercel team with its security and compliance settings.
 *
 * <p>Returned by {@code GET /v2/teams} and {@code GET /v2/teams/{teamId}}.
 *
 * @author Blazebit
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Team {

	private String id;
	private String slug;
	private String name;
	private String creatorId;
	private SamlConfig saml;
	private DeploymentProtection defaultDeploymentProtection;
	private String sensitiveEnvironmentVariablePolicy;
	private Boolean hideIpAddresses;
	private Boolean hideIpAddressesInLogDrains;
	private Long createdAt;
	private Long updatedAt;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCreatorId() {
		return creatorId;
	}

	public void setCreatorId(String creatorId) {
		this.creatorId = creatorId;
	}

	/** SAML SSO configuration for this team. */
	public SamlConfig getSaml() {
		return saml;
	}

	public void setSaml(SamlConfig saml) {
		this.saml = saml;
	}

	/** Default deployment protection settings applied to new projects in this team. */
	public DeploymentProtection getDefaultDeploymentProtection() {
		return defaultDeploymentProtection;
	}

	public void setDefaultDeploymentProtection(DeploymentProtection defaultDeploymentProtection) {
		this.defaultDeploymentProtection = defaultDeploymentProtection;
	}

	/** Whether sensitive environment variables are enabled: {@code on}, {@code off}, or {@code null} for default. */
	public String getSensitiveEnvironmentVariablePolicy() {
		return sensitiveEnvironmentVariablePolicy;
	}

	public void setSensitiveEnvironmentVariablePolicy(String sensitiveEnvironmentVariablePolicy) {
		this.sensitiveEnvironmentVariablePolicy = sensitiveEnvironmentVariablePolicy;
	}

	/** Whether IP addresses are hidden in runtime logs and observability. */
	public Boolean getHideIpAddresses() {
		return hideIpAddresses;
	}

	public void setHideIpAddresses(Boolean hideIpAddresses) {
		this.hideIpAddresses = hideIpAddresses;
	}

	/** Whether IP addresses are hidden in log drain output. */
	public Boolean getHideIpAddressesInLogDrains() {
		return hideIpAddressesInLogDrains;
	}

	public void setHideIpAddressesInLogDrains(Boolean hideIpAddressesInLogDrains) {
		this.hideIpAddressesInLogDrains = hideIpAddressesInLogDrains;
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
	 * SAML SSO and directory sync configuration.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class SamlConfig {

		private SamlConnection connection;
		private Boolean enforced;
		private DirectorySyncConfig directory;

		/** The SAML identity provider connection. */
		public SamlConnection getConnection() {
			return connection;
		}

		public void setConnection(SamlConnection connection) {
			this.connection = connection;
		}

		/** Whether SAML authentication is enforced for all team members. */
		public Boolean getEnforced() {
			return enforced;
		}

		public void setEnforced(Boolean enforced) {
			this.enforced = enforced;
		}

		/** Directory sync (SCIM) configuration. */
		public DirectorySyncConfig getDirectory() {
			return directory;
		}

		public void setDirectory(DirectorySyncConfig directory) {
			this.directory = directory;
		}
	}

	/**
	 * SAML identity provider connection details.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class SamlConnection {

		private String type;
		private String state;
		private String status;

		/** Identity provider type (e.g. {@code OktaSAML}, {@code AzureSAML}). */
		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		/** Connection state: {@code active}, {@code inactive}, or {@code error}. */
		public String getState() {
			return state;
		}

		public void setState(String state) {
			this.state = state;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}
	}

	/**
	 * Directory sync (SCIM) configuration.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class DirectorySyncConfig {

		private String state;
		private Long lastSyncedAt;

		/** Sync state: {@code SETUP} or {@code ACTIVE}. */
		public String getState() {
			return state;
		}

		public void setState(String state) {
			this.state = state;
		}

		public Long getLastSyncedAt() {
			return lastSyncedAt;
		}

		public void setLastSyncedAt(Long lastSyncedAt) {
			this.lastSyncedAt = lastSyncedAt;
		}
	}

	/**
	 * Deployment protection settings (password and/or SSO protection).
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class DeploymentProtection {

		private ProtectionConfig passwordProtection;
		private ProtectionConfig ssoProtection;

		public ProtectionConfig getPasswordProtection() {
			return passwordProtection;
		}

		public void setPasswordProtection(ProtectionConfig passwordProtection) {
			this.passwordProtection = passwordProtection;
		}

		public ProtectionConfig getSsoProtection() {
			return ssoProtection;
		}

		public void setSsoProtection(ProtectionConfig ssoProtection) {
			this.ssoProtection = ssoProtection;
		}
	}

	/**
	 * A deployment protection option (password or SSO).
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ProtectionConfig {

		private String deploymentType;

		/** Which deployments are protected: {@code all_deployments}, {@code only_preview_deployments}, etc. */
		public String getDeploymentType() {
			return deploymentType;
		}

		public void setDeploymentType(String deploymentType) {
			this.deploymentType = deploymentType;
		}
	}
}
