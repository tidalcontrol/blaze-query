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

public class DomainDataFetcherTest {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new VercelSchemaProvider() );
		builder.registerSchemaObjectAlias( Domain.class, "VercelDomain" );
		CONTEXT = builder.build();
	}

	@Test
	void should_return_all_domains() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( Domain.class, List.of(
					VercelTestObjects.verifiedDomain(),
					VercelTestObjects.unverifiedDomain(),
					VercelTestObjects.expiringDomain()
			) );

			var result = session.createQuery(
					"SELECT d.id, d.name, d.verified FROM VercelDomain d",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}

	@Test
	void should_find_unverified_domains() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( Domain.class, List.of(
					VercelTestObjects.verifiedDomain(),
					VercelTestObjects.unverifiedDomain(),
					VercelTestObjects.expiringDomain()
			) );

			// Unverified domains can indicate misconfiguration or domain takeover risk
			var result = session.createQuery(
					"SELECT d.id, d.name FROM VercelDomain d WHERE d.verified = false OR d.verified IS NULL",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "unverified.example.com" );
		}
	}

	@Test
	void should_find_domains_without_auto_renew() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( Domain.class, List.of(
					VercelTestObjects.verifiedDomain(),
					VercelTestObjects.unverifiedDomain(),
					VercelTestObjects.expiringDomain()
			) );

			// Domains with renew=false that also have an expiry are at hijack risk
			var result = session.createQuery(
					"SELECT d.id, d.name, d.expiresAt FROM VercelDomain d WHERE d.renew = false AND d.expiresAt IS NOT NULL",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "expiring.example.com" );
		}
	}

	@Test
	void should_find_external_service_type_domains() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( Domain.class, List.of(
					VercelTestObjects.verifiedDomain(),
					VercelTestObjects.unverifiedDomain(),
					VercelTestObjects.expiringDomain()
			) );

			var result = session.createQuery(
					"SELECT d.id, d.name FROM VercelDomain d WHERE d.serviceType = 'external'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}
}
