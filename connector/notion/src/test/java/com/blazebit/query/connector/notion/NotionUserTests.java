/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.notion;

import com.blazebit.query.QueryContext;
import com.blazebit.query.TypeReference;
import com.blazebit.query.impl.QueryContextBuilderImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class NotionUserTests {

	private static final QueryContext CONTEXT;

	static {
		var builder = new QueryContextBuilderImpl();
		builder.registerSchemaProvider( new NotionSchemaProvider() );
		builder.registerSchemaObjectAlias( NotionUser.class, "NotionUser" );
		CONTEXT = builder.build();
	}

	private static NotionUser person() {
		return new NotionUser( "user-001", "person", "Alice Example", null, "alice@example.com", null, null );
	}

	private static NotionUser bot() {
		return new NotionUser( "bot-001", "bot", "My Integration", null, null, "user-001", "user" );
	}

	private static NotionUser workspaceBot() {
		return new NotionUser( "bot-002", "bot", "Workspace Bot", null, null, null, "workspace" );
	}

	@Test
	void should_return_all_users() {
		try (var session = CONTEXT.createSession()) {
			session.put( NotionUser.class, List.of( person(), bot(), workspaceBot() ) );

			var result = session.createQuery(
					"SELECT u.id, u.name, u.type FROM NotionUser u",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 3 );
		}
	}

	@Test
	void should_filter_persons() {
		try (var session = CONTEXT.createSession()) {
			session.put( NotionUser.class, List.of( person(), bot(), workspaceBot() ) );

			var result = session.createQuery(
					"SELECT u.id, u.name, u.email FROM NotionUser u WHERE u.type = 'person'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "email" ) ).isEqualTo( "alice@example.com" );
		}
	}

	@Test
	void should_filter_bots() {
		try (var session = CONTEXT.createSession()) {
			session.put( NotionUser.class, List.of( person(), bot(), workspaceBot() ) );

			var result = session.createQuery(
					"SELECT u.id, u.name, u.botOwnerType FROM NotionUser u WHERE u.type = 'bot'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}

	@Test
	void should_find_workspace_owned_bots() {
		try (var session = CONTEXT.createSession()) {
			session.put( NotionUser.class, List.of( person(), bot(), workspaceBot() ) );

			var result = session.createQuery(
					"SELECT u.id, u.name FROM NotionUser u WHERE u.type = 'bot' AND u.botOwnerType = 'workspace'",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).get( "name" ) ).isEqualTo( "Workspace Bot" );
		}
	}

	@Test
	void should_count_users_by_type() {
		try (var session = CONTEXT.createSession()) {
			session.put( NotionUser.class, List.of( person(), bot(), workspaceBot() ) );

			var result = session.createQuery(
					"SELECT u.type, COUNT(*) AS total FROM NotionUser u GROUP BY u.type",
					new TypeReference<Map<String, Object>>() {} ).getResultList();

			assertThat( result ).hasSize( 2 );
		}
	}
}
