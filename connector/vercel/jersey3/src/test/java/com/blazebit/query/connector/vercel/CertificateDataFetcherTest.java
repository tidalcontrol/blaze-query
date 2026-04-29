/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.vercel;

import com.blazebit.query.QueryContext;
import com.blazebit.query.TypeReference;
import com.blazebit.query.impl.QueryContextBuilderImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CertificateDataFetcherTest {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new VercelSchemaProvider() );
		builder.registerSchemaObjectAlias( Certificate.class, "VercelCertificate" );
		CONTEXT = builder.build();
	}

	@Test
	void should_return_all_certificates() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( Certificate.class, List.of(
					VercelTestObjects.validCertificate(),
					VercelTestObjects.expiringCertificate()
			) );

			var result = session.createQuery(
					"SELECT c.id, c.autoRenew, c.expiresAt FROM VercelCertificate c",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_certificates_without_auto_renew() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( Certificate.class, List.of(
					VercelTestObjects.validCertificate(),
					VercelTestObjects.expiringCertificate()
			) );

			var result = session.createQuery(
					"SELECT c.id FROM VercelCertificate c WHERE c.autoRenew = false OR c.autoRenew IS NULL",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
		}
	}

	@Test
	void should_find_certificates_expiring_soon() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( Certificate.class, List.of(
					VercelTestObjects.validCertificate(),
					VercelTestObjects.expiringCertificate()
			) );

			// Expires within 30 days (now = ~1_712_000_000_000L in tests)
			long thirtyDaysMs = 30L * 24 * 60 * 60 * 1000;
			long now = 1_712_000_000_000L;
			long threshold = now + thirtyDaysMs;

			var result = session.createQuery(
					"SELECT c.id FROM VercelCertificate c WHERE c.expiresAt < " + threshold,
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
		}
	}
}
