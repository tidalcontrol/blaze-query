/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.blazebit.query.spi.ConfigurationProvider;
import com.blazebit.query.spi.DataFetcher;
import com.blazebit.query.spi.QuerySchemaProvider;

import java.util.Set;

/**
 * Schema provider for the Scaleway connector.
 * Registers all available DataFetcher instances covering the IAM, Instance,
 * Secret Manager, Key Manager, Audit Trail, Kubernetes, Container Registry,
 * and VPC APIs.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public final class ScalewaySchemaProvider implements QuerySchemaProvider {

	@Override
	public Set<? extends DataFetcher<?>> resolveSchemaObjects(ConfigurationProvider configurationProvider) {
		return Set.of(
				// IAM — identity, access, and credential management
				ScalewayIamUserDataFetcher.INSTANCE,
				ScalewayIamGroupDataFetcher.INSTANCE,
				ScalewayIamApplicationDataFetcher.INSTANCE,
				ScalewayIamApiKeyDataFetcher.INSTANCE,
				ScalewayIamPolicyDataFetcher.INSTANCE,
				ScalewayIamSshKeyDataFetcher.INSTANCE,
				// Instance — compute and network security
				ScalewayInstanceDataFetcher.INSTANCE,
				ScalewaySecurityGroupDataFetcher.INSTANCE,
				ScalewaySecurityGroupRuleDataFetcher.INSTANCE,
				// Secret Manager — secret existence and version tracking
				ScalewaySecretDataFetcher.INSTANCE,
				ScalewaySecretVersionDataFetcher.INSTANCE,
				// Key Manager (KMS) — encryption key hygiene
				ScalewayKmsKeyDataFetcher.INSTANCE,
				// Audit Trail — security investigation and compliance reporting
				ScalewayAuditEventDataFetcher.INSTANCE,
				// Kubernetes — cluster version and network isolation
				ScalewayK8sClusterDataFetcher.INSTANCE,
				// Container Registry — image visibility and namespace access
				ScalewayRegistryNamespaceDataFetcher.INSTANCE,
				ScalewayRegistryImageDataFetcher.INSTANCE,
				// VPC — network segmentation and private network usage
				ScalewayVpcDataFetcher.INSTANCE,
				ScalewayPrivateNetworkDataFetcher.INSTANCE,
				// Object Storage — public bucket exposure and data hygiene
				ScalewayObjectStorageBucketDataFetcher.INSTANCE,
				// Managed Databases — public endpoints, HA, backup retention
				ScalewayDatabaseDataFetcher.INSTANCE,
				// Serverless — public endpoints, env-var secret leakage
				ScalewayContainerDataFetcher.INSTANCE,
				ScalewayFunctionDataFetcher.INSTANCE,
				// Block Storage — orphaned volumes and snapshots
				ScalewayVolumeDataFetcher.INSTANCE,
				ScalewaySnapshotDataFetcher.INSTANCE,
				// Load Balancers — TLS enforcement and frontend security
				ScalewayLoadBalancerDataFetcher.INSTANCE,
				ScalewayLoadBalancerFrontendDataFetcher.INSTANCE,
				// Flexible IPs — unattached IPs reducing attack surface
				ScalewayFlexibleIpDataFetcher.INSTANCE,
				// Cockpit — observability and alerting coverage
				ScalewayCockpitAlertManagerDataFetcher.INSTANCE,
				// Transactional Email — SPF/DKIM/MX domain hygiene
				ScalewayTemDomainDataFetcher.INSTANCE
		);
	}
}
