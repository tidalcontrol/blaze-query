/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.azure.defender;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a Microsoft Defender Antivirus health record for a single device,
 * as returned by the {@code /api/deviceavinfo} export API.
 *
 * <p>{@link #avMode} is a string-typed integer: {@code 0}=Active, {@code 1}=Passive,
 * {@code 2}=Disabled, {@code 4}=EDRBlocked, {@code 5}=PassiveAudit.
 *
 * @author Felix Hagemans
 * @since 2.4.4
 */
public record DefenderMachineAvHealth(
		String tenantId,
		String id,
		String machineId,
		String computerDnsName,
		String osKind,
		String osPlatform,
		String osVersion,
		String avMode,
		String avSignatureVersion,
		String avEngineVersion,
		String avPlatformVersion,
		String avIsSignatureUpToDate,
		String avIsEngineUpToDate,
		String avIsPlatformUpToDate,
		String avSignatureUpdateTime,
		String avEngineUpdateTime,
		String avPlatformUpdateTime,
		String avSignaturePublishTime,
		String quickScanResult,
		String quickScanError,
		String quickScanTime,
		String fullScanResult,
		String fullScanError,
		String fullScanTime,
		String lastSeenTime,
		String dataRefreshTimestamp,
		Long rbacGroupId,
		String rbacGroupName) {

	static DefenderMachineAvHealth fromJson(String tenantId, JsonNode node) {
		return new DefenderMachineAvHealth(
				tenantId,
				textOrNull( node, "id" ),
				textOrNull( node, "machineId" ),
				textOrNull( node, "computerDnsName" ),
				textOrNull( node, "osKind" ),
				textOrNull( node, "osPlatform" ),
				textOrNull( node, "osVersion" ),
				textOrNull( node, "avMode" ),
				textOrNull( node, "avSignatureVersion" ),
				textOrNull( node, "avEngineVersion" ),
				textOrNull( node, "avPlatformVersion" ),
				textOrNull( node, "avIsSignatureUpToDate" ),
				textOrNull( node, "avIsEngineUpToDate" ),
				textOrNull( node, "avIsPlatformUpToDate" ),
				textOrNull( node, "avSignatureUpdateTime" ),
				textOrNull( node, "avEngineUpdateTime" ),
				textOrNull( node, "avPlatformUpdateTime" ),
				textOrNull( node, "avSignaturePublishTime" ),
				textOrNull( node, "quickScanResult" ),
				textOrNull( node, "quickScanError" ),
				textOrNull( node, "quickScanTime" ),
				textOrNull( node, "fullScanResult" ),
				textOrNull( node, "fullScanError" ),
				textOrNull( node, "fullScanTime" ),
				textOrNull( node, "lastSeenTime" ),
				textOrNull( node, "dataRefreshTimestamp" ),
				longOrNull( node, "rbacGroupId" ),
				textOrNull( node, "rbacGroupName" ) );
	}

	private static String textOrNull(JsonNode node, String field) {
		JsonNode f = node.get( field );
		return f == null || f.isNull() ? null : f.asText();
	}

	private static Long longOrNull(JsonNode node, String field) {
		JsonNode f = node.get( field );
		return f == null || f.isNull() ? null : f.asLong();
	}
}
