/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.vercel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * The Vercel Web Application Firewall (WAF) configuration for a project.
 *
 * <p>Returned by {@code GET /v1/security/firewall/config/latest} scoped to
 * a project via the {@code projectIdOrName} query parameter.
 *
 * <p>Security-relevant fields: {@code firewallEnabled}, managed rule sets
 * (OWASP, bot protection), and custom IP block/allow rules.
 *
 * @author Blazebit
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FirewallConfig {

	private String id;
	private String ownerId;
	private String projectKey;
	private String projectId;
	private Boolean firewallEnabled;
	private Boolean botIdEnabled;
	private ManagedRules managedRules;
	private List<FirewallRule> rules;
	private List<IpRule> ips;
	private String updatedAt;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public String getProjectKey() {
		return projectKey;
	}

	public void setProjectKey(String projectKey) {
		this.projectKey = projectKey;
	}

	/** The project ID this config belongs to (set by the fetcher). */
	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	/** Whether the WAF is enabled for this project. */
	public Boolean getFirewallEnabled() {
		return firewallEnabled;
	}

	public void setFirewallEnabled(Boolean firewallEnabled) {
		this.firewallEnabled = firewallEnabled;
	}

	/** Whether bot ID detection is enabled. */
	public Boolean getBotIdEnabled() {
		return botIdEnabled;
	}

	public void setBotIdEnabled(Boolean botIdEnabled) {
		this.botIdEnabled = botIdEnabled;
	}

	/** Managed rule set configuration (OWASP, bot protection, Vercel ruleset). */
	public ManagedRules getManagedRules() {
		return managedRules;
	}

	public void setManagedRules(ManagedRules managedRules) {
		this.managedRules = managedRules;
	}

	/** Custom firewall rules defined by the team. */
	public List<FirewallRule> getRules() {
		return rules;
	}

	public void setRules(List<FirewallRule> rules) {
		this.rules = rules;
	}

	/** IP-level block or allow rules. */
	public List<IpRule> getIps() {
		return ips;
	}

	public void setIps(List<IpRule> ips) {
		this.ips = ips;
	}

	public String getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(String updatedAt) {
		this.updatedAt = updatedAt;
	}

	/**
	 * Managed rule set configuration.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ManagedRules {

		private ManagedRule owaspCoruleSet;
		private ManagedRule botProtection;
		private ManagedRule vercelRuleset;
		private ManagedRule aiBots;

		public ManagedRule getOwaspCoruleSet() {
			return owaspCoruleSet;
		}

		public void setOwaspCoruleSet(ManagedRule owaspCoruleSet) {
			this.owaspCoruleSet = owaspCoruleSet;
		}

		public ManagedRule getBotProtection() {
			return botProtection;
		}

		public void setBotProtection(ManagedRule botProtection) {
			this.botProtection = botProtection;
		}

		public ManagedRule getVercelRuleset() {
			return vercelRuleset;
		}

		public void setVercelRuleset(ManagedRule vercelRuleset) {
			this.vercelRuleset = vercelRuleset;
		}

		public ManagedRule getAiBots() {
			return aiBots;
		}

		public void setAiBots(ManagedRule aiBots) {
			this.aiBots = aiBots;
		}
	}

	/**
	 * A single managed rule set toggle.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ManagedRule {

		private Boolean active;
		private String action;

		/** Whether this managed rule set is active. */
		public Boolean getActive() {
			return active;
		}

		public void setActive(Boolean active) {
			this.active = active;
		}

		/** The action taken when matched: {@code deny} or {@code log}. */
		public String getAction() {
			return action;
		}

		public void setAction(String action) {
			this.action = action;
		}
	}

	/**
	 * A custom firewall rule.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class FirewallRule {

		private String id;
		private String name;
		private Boolean active;
		private String action;

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

		public Boolean getActive() {
			return active;
		}

		public void setActive(Boolean active) {
			this.active = active;
		}

		/** Rule action: {@code deny}, {@code log}, {@code challenge}, or {@code bypass}. */
		public String getAction() {
			return action;
		}

		public void setAction(String action) {
			this.action = action;
		}
	}

	/**
	 * An IP-level firewall rule.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class IpRule {

		private String id;
		private String hostname;
		private String ip;
		private String notes;
		private String action;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getHostname() {
			return hostname;
		}

		public void setHostname(String hostname) {
			this.hostname = hostname;
		}

		public String getIp() {
			return ip;
		}

		public void setIp(String ip) {
			this.ip = ip;
		}

		public String getNotes() {
			return notes;
		}

		public void setNotes(String notes) {
			this.notes = notes;
		}

		/** Action: {@code deny} or {@code allow}. */
		public String getAction() {
			return action;
		}

		public void setAction(String action) {
			this.action = action;
		}
	}
}
