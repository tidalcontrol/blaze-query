/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.vercel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * A TLS certificate managed by Vercel.
 *
 * <p>Returned by {@code GET /v7/certs}.
 *
 * <p>Security-relevant: {@code expiresAt} for expiry monitoring and
 * {@code autoRenew} to detect certificates that will not be renewed automatically.
 *
 * @author Blazebit
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Certificate {

	private String id;
	private List<String> cns;
	private Boolean autoRenew;
	private Long expiresAt;
	private Long createdAt;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	/** The common names (domains) this certificate covers. */
	public List<String> getCns() {
		return cns;
	}

	public void setCns(List<String> cns) {
		this.cns = cns;
	}

	/** Whether Vercel will automatically renew this certificate before it expires. */
	public Boolean getAutoRenew() {
		return autoRenew;
	}

	public void setAutoRenew(Boolean autoRenew) {
		this.autoRenew = autoRenew;
	}

	/** Unix timestamp (ms) when the certificate expires. */
	public Long getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Long expiresAt) {
		this.expiresAt = expiresAt;
	}

	public Long getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Long createdAt) {
		this.createdAt = createdAt;
	}
}
