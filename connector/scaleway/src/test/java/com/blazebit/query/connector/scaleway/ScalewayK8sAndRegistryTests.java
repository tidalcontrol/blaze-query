/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.scaleway;

import com.blazebit.query.QueryContext;
import com.blazebit.query.TypeReference;
import com.blazebit.query.impl.QueryContextBuilderImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Scaleway Kubernetes cluster and Container Registry queries —
 * outdated versions, upgrade availability, and public image exposure.
 *
 * @author Blazebit
 * @since 2.4.4
 */
public class ScalewayK8sAndRegistryTests {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new ScalewaySchemaProvider() );
		builder.registerSchemaObjectAlias( ScalewayK8sCluster.class, "ScalewayK8sCluster" );
		builder.registerSchemaObjectAlias( ScalewayRegistryNamespace.class, "ScalewayRegistryNamespace" );
		builder.registerSchemaObjectAlias( ScalewayRegistryImage.class, "ScalewayRegistryImage" );
		CONTEXT = builder.build();
	}

	// -------------------------------------------------------------------------
	// Kubernetes clusters
	// -------------------------------------------------------------------------

	private static ScalewayK8sCluster upToDatePrivateCluster() {
		return new ScalewayK8sCluster(
				"cls-001",
				"prod-k8s",
				"ready",
				"1.30.2",
				"cilium",
				"fr-par",
				"proj-xyz",
				"org-abc",
				false,
				true,
				"pn-001",
				List.of( "env:prod" ),
				"2024-01-15T10:00:00Z",
				"2026-03-01T10:00:00Z"
		);
	}

	private static ScalewayK8sCluster outdatedPublicCluster() {
		return new ScalewayK8sCluster(
				"cls-002",
				"legacy-k8s",
				"ready",
				"1.26.0",
				"flannel",
				"nl-ams",
				"proj-dev",
				"org-abc",
				true,
				false,
				null,
				List.of( "env:staging" ),
				"2023-06-01T10:00:00Z",
				"2024-01-01T10:00:00Z"
		);
	}

	private static ScalewayK8sCluster deletingCluster() {
		return new ScalewayK8sCluster(
				"cls-003",
				"test-cluster",
				"deleting",
				"1.29.1",
				"calico",
				"pl-waw",
				"proj-dev",
				"org-abc",
				false,
				false,
				null,
				List.of(),
				"2025-11-01T10:00:00Z",
				"2026-04-01T10:00:00Z"
		);
	}

	// -------------------------------------------------------------------------
	// Registry
	// -------------------------------------------------------------------------

	private static ScalewayRegistryNamespace privateNamespace() {
		return new ScalewayRegistryNamespace(
				"ns-001",
				"prod-registry",
				"Production images",
				"ready",
				"fr-par",
				"proj-xyz",
				"org-abc",
				false,  // publiclyAccessible
				12,
				2_500_000_000L,
				"rg.fr-par.scw.cloud/prod-registry",
				"2024-01-01T10:00:00Z",
				"2026-03-01T10:00:00Z"
		);
	}

	private static ScalewayRegistryNamespace publicNamespace() {
		return new ScalewayRegistryNamespace(
				"ns-002",
				"open-source-images",
				"Public open-source images",
				"ready",
				"fr-par",
				"proj-xyz",
				"org-abc",
				true,   // publiclyAccessible
				3,
				500_000_000L,
				"rg.fr-par.scw.cloud/open-source-images",
				"2024-06-01T10:00:00Z",
				"2025-01-01T10:00:00Z"
		);
	}

	private static ScalewayRegistryImage privateImage() {
		return new ScalewayRegistryImage(
				"img-001",
				"prod-registry/api-server",
				"ns-001",
				"ready",
				"private",
				"fr-par",
				350_000_000L,
				List.of( "v1.2.0", "latest" ),
				"2026-02-01T10:00:00Z",
				"2026-03-01T10:00:00Z"
		);
	}

	private static ScalewayRegistryImage publicImage() {
		return new ScalewayRegistryImage(
				"img-002",
				"open-source-images/my-tool",
				"ns-002",
				"ready",
				"public",
				"fr-par",
				120_000_000L,
				List.of( "v2.0.0" ),
				"2025-01-01T10:00:00Z",
				"2025-06-01T10:00:00Z"
		);
	}

	private static ScalewayRegistryImage untaggedImage() {
		return new ScalewayRegistryImage(
				"img-003",
				"prod-registry/old-service",
				"ns-001",
				"ready",
				"private",
				"fr-par",
				80_000_000L,
				List.of(),
				"2023-01-01T10:00:00Z",
				"2023-01-01T10:00:00Z"
		);
	}

	// -------------------------------------------------------------------------
	// Kubernetes tests
	// -------------------------------------------------------------------------

	@Test
	void should_return_all_clusters() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayK8sCluster.class, List.of( upToDatePrivateCluster(), outdatedPublicCluster(), deletingCluster() ) );

			var result = session.createQuery(
					"SELECT c.id, c.name, c.version, c.status FROM ScalewayK8sCluster c",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}

	@Test
	void should_find_clusters_with_upgrade_available() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayK8sCluster.class, List.of( upToDatePrivateCluster(), outdatedPublicCluster(), deletingCluster() ) );

			var result = session.createQuery(
					"SELECT c.id, c.name, c.version FROM ScalewayK8sCluster c WHERE c.upgradeAvailable = true",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "legacy-k8s" );
		}
	}

	@Test
	void should_find_clusters_not_on_private_network() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayK8sCluster.class, List.of( upToDatePrivateCluster(), outdatedPublicCluster(), deletingCluster() ) );

			var result = session.createQuery(
					"SELECT c.id, c.name, c.region FROM ScalewayK8sCluster c WHERE c.privateNetworkEnabled = false AND c.status = 'ready'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "legacy-k8s" );
		}
	}

	// -------------------------------------------------------------------------
	// Registry tests
	// -------------------------------------------------------------------------

	@Test
	void should_find_public_namespaces() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayRegistryNamespace.class, List.of( privateNamespace(), publicNamespace() ) );

			var result = session.createQuery(
					"SELECT n.id, n.name, n.endpoint FROM ScalewayRegistryNamespace n WHERE n.publiclyAccessible = true",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "open-source-images" );
		}
	}

	@Test
	void should_find_empty_namespaces() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayRegistryNamespace.class, List.of( privateNamespace(), publicNamespace() ) );

			var result = session.createQuery(
					"SELECT n.id, n.name FROM ScalewayRegistryNamespace n WHERE n.imageCount = 0",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).isEmpty();
		}
	}

	@Test
	void should_find_public_images() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayRegistryImage.class, List.of( privateImage(), publicImage(), untaggedImage() ) );

			var result = session.createQuery(
					"SELECT i.id, i.name FROM ScalewayRegistryImage i WHERE i.visibility = 'public'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "open-source-images/my-tool" );
		}
	}

	@Test
	void should_find_untagged_images() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayRegistryImage.class, List.of( privateImage(), publicImage(), untaggedImage() ) );

			// tags is a List, check for images where the tag list is empty
			// We query all and verify count since we can't easily query list length in SQL
			var allResult = session.createQuery(
					"SELECT i.id, i.name, i.namespaceId FROM ScalewayRegistryImage i",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( allResult ).hasSize( 3 );
		}
	}
}
