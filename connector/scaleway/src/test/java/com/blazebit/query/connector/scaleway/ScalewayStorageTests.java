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
 * Tests for Scaleway storage queries — Object Storage buckets, block volumes, and snapshots.
 *
 * @author Martijn Sprengers
 * @since 2.4.4
 */
public class ScalewayStorageTests {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new ScalewaySchemaProvider() );
		builder.registerSchemaObjectAlias( ScalewayObjectStorageBucket.class, "ScalewayObjectStorageBucket" );
		builder.registerSchemaObjectAlias( ScalewayVolume.class, "ScalewayVolume" );
		builder.registerSchemaObjectAlias( ScalewaySnapshot.class, "ScalewaySnapshot" );
		CONTEXT = builder.build();
	}

	// -------------------------------------------------------------------------
	// Object Storage Bucket test objects
	// -------------------------------------------------------------------------

	private static ScalewayObjectStorageBucket largeBucket() {
		return new ScalewayObjectStorageBucket(
				"prod-data",
				"fr-par",
				"proj-xyz",
				"org-abc",
				1500L,
				5_000_000_000L,
				List.of( "env:prod" ),
				"2024-01-01T00:00:00Z",
				"2026-03-01T00:00:00Z"
		);
	}

	private static ScalewayObjectStorageBucket emptyBucket() {
		return new ScalewayObjectStorageBucket(
				"staging-empty",
				"nl-ams",
				"proj-xyz",
				"org-abc",
				0L,
				0L,
				List.of(),
				"2024-06-01T00:00:00Z",
				"2024-06-01T00:00:00Z"
		);
	}

	private static ScalewayObjectStorageBucket devBucket() {
		return new ScalewayObjectStorageBucket(
				"dev-assets",
				"fr-par",
				"proj-xyz",
				"org-abc",
				42L,
				500_000L,
				List.of( "env:dev" ),
				"2025-01-01T00:00:00Z",
				"2025-12-01T00:00:00Z"
		);
	}

	@Test
	void should_return_all_buckets() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayObjectStorageBucket.class, List.of( largeBucket(), emptyBucket(), devBucket() ) );

			var result = session.createQuery(
					"SELECT b.name, b.region, b.objectCount, b.totalSize FROM ScalewayObjectStorageBucket b",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}

	@Test
	void should_find_empty_buckets() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayObjectStorageBucket.class, List.of( largeBucket(), emptyBucket(), devBucket() ) );

			var result = session.createQuery(
					"SELECT b.name, b.region FROM ScalewayObjectStorageBucket b WHERE b.objectCount = 0",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "staging-empty" );
		}
	}

	@Test
	void should_find_large_buckets() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayObjectStorageBucket.class, List.of( largeBucket(), emptyBucket(), devBucket() ) );

			var result = session.createQuery(
					"SELECT b.name, b.objectCount FROM ScalewayObjectStorageBucket b WHERE b.objectCount > 1000",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
		}
	}

	// -------------------------------------------------------------------------
	// Volume test objects
	// -------------------------------------------------------------------------

	private static ScalewayVolume attachedVolume() {
		return new ScalewayVolume(
				"vol-001",
				"prod-data",
				"b_ssd",
				20_000_000_000L,
				"available",
				"fr-par-1",
				"proj-xyz",
				"org-abc",
				"srv-001",
				"2024-01-01T00:00:00Z"
		);
	}

	private static ScalewayVolume unattachedVolume() {
		return new ScalewayVolume(
				"vol-002",
				"orphan-data",
				"l_ssd",
				10_000_000_000L,
				"available",
				"fr-par-1",
				"proj-xyz",
				"org-abc",
				null,
				"2024-03-01T00:00:00Z"
		);
	}

	private static ScalewayVolume snapshotVolume() {
		return new ScalewayVolume(
				"vol-003",
				"backup-vol",
				"b_ssd",
				20_000_000_000L,
				"snapshotting",
				"fr-par-1",
				"proj-xyz",
				"org-abc",
				null,
				"2024-06-01T00:00:00Z"
		);
	}

	@Test
	void should_return_all_volumes() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayVolume.class, List.of( attachedVolume(), unattachedVolume(), snapshotVolume() ) );

			var result = session.createQuery(
					"SELECT v.id, v.name, v.volumeType, v.size FROM ScalewayVolume v",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}

	@Test
	void should_find_unattached_volumes() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayVolume.class, List.of( attachedVolume(), unattachedVolume(), snapshotVolume() ) );

			var result = session.createQuery(
					"SELECT v.id, v.name FROM ScalewayVolume v WHERE v.serverId IS NULL",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_volumes_by_type() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewayVolume.class, List.of( attachedVolume(), unattachedVolume(), snapshotVolume() ) );

			var result = session.createQuery(
					"SELECT v.id, v.name FROM ScalewayVolume v WHERE v.volumeType = 'l_ssd'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
		}
	}

	// -------------------------------------------------------------------------
	// Snapshot test objects
	// -------------------------------------------------------------------------

	private static ScalewaySnapshot availableSnapshot() {
		return new ScalewaySnapshot(
				"snap-001",
				"prod-backup-2026",
				"available",
				20_000_000_000L,
				"b_ssd",
				"fr-par-1",
				"proj-xyz",
				"org-abc",
				List.of( "env:prod" ),
				"2026-01-01T00:00:00Z"
		);
	}

	private static ScalewaySnapshot oldSnapshot() {
		return new ScalewaySnapshot(
				"snap-002",
				"legacy-backup-2023",
				"available",
				10_000_000_000L,
				"l_ssd",
				"nl-ams-1",
				"proj-xyz",
				"org-abc",
				List.of(),
				"2023-06-01T00:00:00Z"
		);
	}

	@Test
	void should_return_all_snapshots() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewaySnapshot.class, List.of( availableSnapshot(), oldSnapshot() ) );

			var result = session.createQuery(
					"SELECT s.id, s.name, s.state, s.zone FROM ScalewaySnapshot s",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_old_snapshots() {
		try (var session = CONTEXT.createSession()) {
			session.put( ScalewaySnapshot.class, List.of( availableSnapshot(), oldSnapshot() ) );

			var result = session.createQuery(
					"SELECT s.id, s.name FROM ScalewaySnapshot s WHERE s.createdAt < '2024-01-01T00:00:00Z'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
		}
	}
}
