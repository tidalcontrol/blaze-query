/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.vercel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A domain registered or configured in Vercel.
 *
 * <p>Returned by {@code GET /v5/domains}.
 *
 * <p>Security-relevant: {@code verified} (unverified domains may indicate
 * misconfiguration or domain takeover risk), {@code expiresAt} (expired domains
 * can be hijacked), and {@code serviceType} (external DNS vs Vercel-managed).
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Domain {

	private String id;
	private String name;
	private Boolean verified;
	private String serviceType;
	private String teamId;
	private String userId;
	private Boolean renew;
	private Long expiresAt;
	private Long boughtAt;
	private Long transferredAt;
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

	/** Whether the domain ownership has been verified via DNS or file challenge. */
	public Boolean getVerified() {
		return verified;
	}

	public void setVerified(Boolean verified) {
		this.verified = verified;
	}

	/**
	 * How the domain's DNS is managed: {@code zeit.world} (Vercel DNS),
	 * {@code external} (CNAME/A record), or {@code na}.
	 */
	public String getServiceType() {
		return serviceType;
	}

	public void setServiceType(String serviceType) {
		this.serviceType = serviceType;
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

	/** Whether auto-renew is enabled. Disabled on domains close to expiry is high risk. */
	public Boolean getRenew() {
		return renew;
	}

	public void setRenew(Boolean renew) {
		this.renew = renew;
	}

	/** Unix timestamp (ms) when the domain registration expires; {@code null} for external domains. */
	public Long getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Long expiresAt) {
		this.expiresAt = expiresAt;
	}

	/** Unix timestamp (ms) when the domain was purchased, or {@code null} if externally owned. */
	public Long getBoughtAt() {
		return boughtAt;
	}

	public void setBoughtAt(Long boughtAt) {
		this.boughtAt = boughtAt;
	}

	/** Unix timestamp (ms) of the most recent transfer, or {@code null}. */
	public Long getTransferredAt() {
		return transferredAt;
	}

	public void setTransferredAt(Long transferredAt) {
		this.transferredAt = transferredAt;
	}

	public Long getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Long createdAt) {
		this.createdAt = createdAt;
	}
}
