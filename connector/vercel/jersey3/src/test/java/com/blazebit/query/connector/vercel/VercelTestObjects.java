/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */
package com.blazebit.query.connector.vercel;

import java.util.List;

/**
 * Shared test fixtures for Vercel connector tests.
 */
final class VercelTestObjects {

	private VercelTestObjects() {
	}

	// -------------------------------------------------------------------------
	// AuthToken fixtures
	// -------------------------------------------------------------------------

	static AuthToken activeToken() {
		AuthToken t = new AuthToken();
		t.setId( "tok_abc123" );
		t.setName( "CI Deploy Token" );
		t.setType( "authentication-token" );
		t.setOrigin( "manual" );
		t.setActiveAt( 1_700_000_000_000L );
		t.setCreatedAt( 1_680_000_000_000L );
		t.setScopes( List.of( userScope() ) );
		return t;
	}

	static AuthToken expiringToken() {
		AuthToken t = new AuthToken();
		t.setId( "tok_def456" );
		t.setName( "Short-lived token" );
		t.setType( "authentication-token" );
		t.setOrigin( "saml" );
		t.setExpiresAt( 1_701_000_000_000L );
		t.setActiveAt( 1_700_500_000_000L );
		t.setCreatedAt( 1_700_000_000_000L );
		return t;
	}

	static AuthToken neverUsedToken() {
		AuthToken t = new AuthToken();
		t.setId( "tok_ghi789" );
		t.setName( "Unused legacy token" );
		t.setType( "authentication-token" );
		t.setOrigin( "manual" );
		t.setCreatedAt( 1_600_000_000_000L );
		// activeAt intentionally null — never used
		return t;
	}

	private static AuthToken.TokenScope userScope() {
		AuthToken.TokenScope s = new AuthToken.TokenScope();
		s.setType( "user" );
		s.setOrigin( "manual" );
		s.setCreatedAt( 1_680_000_000_000L );
		return s;
	}

	// -------------------------------------------------------------------------
	// Team fixtures
	// -------------------------------------------------------------------------

	static Team teamWithSamlEnforced() {
		Team t = new Team();
		t.setId( "team_aaaaaa" );
		t.setSlug( "acme-security" );
		t.setName( "Acme Security" );
		t.setCreatorId( "user_001" );
		t.setSaml( samlEnforced() );
		t.setSensitiveEnvironmentVariablePolicy( "on" );
		t.setHideIpAddresses( true );
		t.setHideIpAddressesInLogDrains( true );
		t.setCreatedAt( 1_600_000_000_000L );
		return t;
	}

	static Team teamWithSamlNotEnforced() {
		Team t = new Team();
		t.setId( "team_bbbbbb" );
		t.setSlug( "acme-eng" );
		t.setName( "Acme Engineering" );
		t.setCreatorId( "user_002" );
		t.setSaml( samlNotEnforced() );
		t.setSensitiveEnvironmentVariablePolicy( "off" );
		t.setHideIpAddresses( false );
		t.setCreatedAt( 1_610_000_000_000L );
		return t;
	}

	private static Team.SamlConfig samlEnforced() {
		Team.SamlConfig cfg = new Team.SamlConfig();
		cfg.setEnforced( true );
		Team.SamlConnection conn = new Team.SamlConnection();
		conn.setType( "OktaSAML" );
		conn.setState( "active" );
		cfg.setConnection( conn );
		Team.DirectorySyncConfig dir = new Team.DirectorySyncConfig();
		dir.setState( "ACTIVE" );
		dir.setLastSyncedAt( 1_700_000_000_000L );
		cfg.setDirectory( dir );
		return cfg;
	}

	private static Team.SamlConfig samlNotEnforced() {
		Team.SamlConfig cfg = new Team.SamlConfig();
		cfg.setEnforced( false );
		return cfg;
	}

	// -------------------------------------------------------------------------
	// TeamMember fixtures
	// -------------------------------------------------------------------------

	static TeamMember ownerMember() {
		TeamMember m = new TeamMember();
		m.setUid( "user_001" );
		m.setEmail( "alice@acme.com" );
		m.setName( "Alice" );
		m.setUsername( "alice" );
		m.setRole( "OWNER" );
		m.setTeamId( "team_aaaaaa" );
		m.setConfirmed( true );
		m.setCreatedAt( 1_600_000_000_000L );
		return m;
	}

	static TeamMember developerMember() {
		TeamMember m = new TeamMember();
		m.setUid( "user_002" );
		m.setEmail( "bob@acme.com" );
		m.setName( "Bob" );
		m.setUsername( "bob" );
		m.setRole( "DEVELOPER" );
		m.setTeamId( "team_aaaaaa" );
		m.setConfirmed( true );
		m.setCreatedAt( 1_650_000_000_000L );
		TeamMember.JoinedFrom jf = new TeamMember.JoinedFrom();
		jf.setOrigin( "saml" );
		jf.setSamlConnectedAt( 1_650_000_000_000L );
		m.setJoinedFrom( jf );
		return m;
	}

	static TeamMember unconfirmedMember() {
		TeamMember m = new TeamMember();
		m.setUid( "user_003" );
		m.setEmail( "charlie@acme.com" );
		m.setName( "Charlie" );
		m.setUsername( "charlie" );
		m.setRole( "MEMBER" );
		m.setTeamId( "team_aaaaaa" );
		m.setConfirmed( false );
		m.setCreatedAt( 1_699_000_000_000L );
		return m;
	}

	// -------------------------------------------------------------------------
	// AccessGroup fixtures
	// -------------------------------------------------------------------------

	static AccessGroup dsyncManagedGroup() {
		AccessGroup g = new AccessGroup();
		g.setAccessGroupId( "ag_111" );
		g.setName( "Security Team" );
		g.setTeamId( "team_aaaaaa" );
		g.setMembersCount( 5 );
		g.setProjectsCount( 3 );
		g.setIsDsyncManaged( true );
		g.setCreatedAt( "2024-01-01T00:00:00.000Z" );
		return g;
	}

	static AccessGroup manualGroup() {
		AccessGroup g = new AccessGroup();
		g.setAccessGroupId( "ag_222" );
		g.setName( "Frontend Devs" );
		g.setTeamId( "team_aaaaaa" );
		g.setMembersCount( 10 );
		g.setProjectsCount( 7 );
		g.setIsDsyncManaged( false );
		g.setCreatedAt( "2024-03-15T10:00:00.000Z" );
		return g;
	}

	// -------------------------------------------------------------------------
	// Webhook fixtures
	// -------------------------------------------------------------------------

	static Webhook securityWebhook() {
		Webhook w = new Webhook();
		w.setId( "wh_001" );
		w.setUrl( "https://siem.acme.com/vercel" );
		w.setEvents( List.of( "firewall.attack", "firewall.system-rule-anomaly", "deployment.checks.failed" ) );
		w.setOwnerId( "team_aaaaaa" );
		w.setTeamId( "team_aaaaaa" );
		w.setProjectIds( List.of() );
		w.setCreatedAt( 1_680_000_000_000L );
		return w;
	}

	static Webhook deploymentOnlyWebhook() {
		Webhook w = new Webhook();
		w.setId( "wh_002" );
		w.setUrl( "https://ci.acme.com/hooks/vercel" );
		w.setEvents( List.of( "deployment.created", "deployment.succeeded" ) );
		w.setOwnerId( "team_aaaaaa" );
		w.setTeamId( "team_aaaaaa" );
		w.setProjectIds( List.of( "prj_xyz" ) );
		w.setCreatedAt( 1_690_000_000_000L );
		return w;
	}

	// -------------------------------------------------------------------------
	// Project fixtures
	// -------------------------------------------------------------------------

	static Project protectedProject() {
		Project p = new Project();
		p.setId( "prj_aaa" );
		p.setName( "customer-portal" );
		p.setAccountId( "team_aaaaaa" );
		p.setFramework( "nextjs" );
		Project.ProtectionConfig pwd = new Project.ProtectionConfig();
		pwd.setDeploymentType( "all_deployments" );
		p.setPasswordProtection( pwd );
		Project.SsoProtectionConfig sso = new Project.SsoProtectionConfig();
		sso.setDeploymentType( "only_preview_deployments" );
		p.setSsoProtection( sso );
		p.setAutoExposeSystemEnvs( false );
		p.setCreatedAt( 1_640_000_000_000L );
		return p;
	}

	static Project unprotectedProject() {
		Project p = new Project();
		p.setId( "prj_bbb" );
		p.setName( "internal-tools" );
		p.setAccountId( "team_aaaaaa" );
		p.setFramework( "vite" );
		// No password or SSO protection
		p.setAutoExposeSystemEnvs( true );
		p.setCreatedAt( 1_660_000_000_000L );
		return p;
	}
}
