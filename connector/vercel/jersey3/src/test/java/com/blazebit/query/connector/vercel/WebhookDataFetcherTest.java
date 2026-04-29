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

public class WebhookDataFetcherTest {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new VercelSchemaProvider() );
		builder.registerSchemaObjectAlias( Webhook.class, "VercelWebhook" );
		CONTEXT = builder.build();
	}

	@Test
	void should_return_all_webhooks() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( Webhook.class, List.of(
					VercelTestObjects.securityWebhook(),
					VercelTestObjects.deploymentOnlyWebhook()
			) );

			var result = session.createQuery(
					"SELECT w.id, w.url FROM VercelWebhook w",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_team_wide_webhooks() {
		try ( var session = CONTEXT.createSession() ) {
			session.put( Webhook.class, List.of(
					VercelTestObjects.securityWebhook(),
					VercelTestObjects.deploymentOnlyWebhook()
			) );

			// Webhooks with no project scoping apply to the entire team
			var result = session.createQuery(
					"SELECT w.id, w.url FROM VercelWebhook w WHERE CARDINALITY(w.projectIds) = 0",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "url" ) ).isEqualTo( "https://siem.acme.com/vercel" );
		}
	}
}
