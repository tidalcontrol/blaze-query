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

	// -------------------------------------------------------------------------
	// EnvironmentVariable fixtures
	// -------------------------------------------------------------------------

	static EnvironmentVariable plainEnvVar() {
		EnvironmentVariable v = new EnvironmentVariable();
		v.setId( "env_001" );
		v.setKey( "APP_URL" );
		v.setType( "plain" );
		v.setTarget( List.of( "production", "preview" ) );
		v.setProjectId( "prj_aaa" );
		v.setCreatedAt( 1_680_000_000_000L );
		return v;
	}

	static EnvironmentVariable encryptedEnvVar() {
		EnvironmentVariable v = new EnvironmentVariable();
		v.setId( "env_002" );
		v.setKey( "DATABASE_URL" );
		v.setType( "encrypted" );
		v.setTarget( List.of( "production" ) );
		v.setProjectId( "prj_aaa" );
		v.setCreatedAt( 1_680_000_000_000L );
		return v;
	}

	static EnvironmentVariable sensitiveEnvVar() {
		EnvironmentVariable v = new EnvironmentVariable();
		v.setId( "env_003" );
		v.setKey( "STRIPE_SECRET_KEY" );
		v.setType( "sensitive" );
		v.setTarget( List.of( "production" ) );
		v.setProjectId( "prj_aaa" );
		v.setCreatedAt( 1_690_000_000_000L );
		return v;
	}

	// -------------------------------------------------------------------------
	// LogDrain fixtures
	// -------------------------------------------------------------------------

	static LogDrain siemLogDrain() {
		LogDrain d = new LogDrain();
		d.setId( "ld_001" );
		d.setName( "SIEM Drain" );
		d.setUrl( "https://siem.acme.com/ingest" );
		d.setDeliveryFormat( "ndjson" );
		d.setSources( List.of( "build", "edge", "lambda", "request", "firewall" ) );
		d.setEnvironments( List.of( "production", "preview" ) );
		d.setProjectIds( List.of() );
		d.setTeamId( "team_aaaaaa" );
		d.setCreatedAt( 1_680_000_000_000L );
		return d;
	}

	static LogDrain buildOnlyLogDrain() {
		LogDrain d = new LogDrain();
		d.setId( "ld_002" );
		d.setName( "Build Logs Only" );
		d.setUrl( "https://logs.acme.com/build" );
		d.setDeliveryFormat( "json" );
		d.setSources( List.of( "build" ) );
		d.setEnvironments( List.of( "production" ) );
		d.setProjectIds( List.of( "prj_aaa" ) );
		d.setTeamId( "team_aaaaaa" );
		d.setCreatedAt( 1_690_000_000_000L );
		return d;
	}

	// -------------------------------------------------------------------------
	// FirewallConfig fixtures
	// -------------------------------------------------------------------------

	static FirewallConfig firewallEnabled() {
		FirewallConfig f = new FirewallConfig();
		f.setId( "fw_001" );
		f.setOwnerId( "team_aaaaaa" );
		f.setProjectId( "prj_aaa" );
		f.setProjectKey( "customer-portal" );
		f.setFirewallEnabled( true );
		f.setBotIdEnabled( true );
		FirewallConfig.ManagedRules mr = new FirewallConfig.ManagedRules();
		FirewallConfig.ManagedRule owasp = new FirewallConfig.ManagedRule();
		owasp.setActive( true );
		owasp.setAction( "deny" );
		mr.setOwaspCoruleSet( owasp );
		f.setManagedRules( mr );
		f.setRules( List.of() );
		f.setIps( List.of() );
		return f;
	}

	static FirewallConfig firewallDisabled() {
		FirewallConfig f = new FirewallConfig();
		f.setId( "fw_002" );
		f.setOwnerId( "team_aaaaaa" );
		f.setProjectId( "prj_no_waf" );
		f.setProjectKey( "internal-tools" );
		f.setFirewallEnabled( false );
		f.setBotIdEnabled( false );
		f.setRules( List.of() );
		f.setIps( List.of() );
		return f;
	}

	// -------------------------------------------------------------------------
	// IntegrationConfiguration fixtures
	// -------------------------------------------------------------------------

	static IntegrationConfiguration activeIntegration() {
		IntegrationConfiguration i = new IntegrationConfiguration();
		i.setId( "icfg_001" );
		i.setSlug( "datadog" );
		i.setIntegrationId( "int_datadog" );
		i.setOwnerId( "team_aaaaaa" );
		i.setTeamId( "team_aaaaaa" );
		i.setStatus( "ready" );
		i.setSource( "marketplace" );
		i.setScopes( List.of( "read:deployments", "read:logs" ) );
		i.setProjects( List.of() ); // team-wide
		i.setCreatedAt( 1_680_000_000_000L );
		i.setUpdatedAt( 1_700_000_000_000L );
		return i;
	}

	static IntegrationConfiguration suspendedIntegration() {
		IntegrationConfiguration i = new IntegrationConfiguration();
		i.setId( "icfg_002" );
		i.setSlug( "legacy-connector" );
		i.setIntegrationId( "int_legacy" );
		i.setOwnerId( "team_aaaaaa" );
		i.setTeamId( "team_aaaaaa" );
		i.setStatus( "suspended" );
		i.setSource( "external" );
		i.setScopes( List.of( "read:deployments", "write:env", "read:logs", "read:users" ) );
		i.setProjects( List.of( "prj_aaa" ) );
		i.setCreatedAt( 1_600_000_000_000L );
		i.setUpdatedAt( 1_650_000_000_000L );
		i.setDisabledAt( 1_700_000_000_000L );
		return i;
	}

	// -------------------------------------------------------------------------
	// Deployment fixtures
	// -------------------------------------------------------------------------

	static Deployment gitDeployment() {
		Deployment d = new Deployment();
		d.setUid( "dpl_git_001" );
		d.setName( "customer-portal" );
		d.setProjectId( "prj_aaa" );
		d.setUrl( "customer-portal-abc.vercel.app" );
		d.setState( "READY" );
		d.setTarget( "production" );
		d.setSource( "git" );
		d.setChecksConclusion( "succeeded" );
		Deployment.Creator creator = new Deployment.Creator();
		creator.setUid( "user_001" );
		creator.setEmail( "alice@acme.com" );
		d.setCreator( creator );
		d.setCreated( 1_710_000_000_000L );
		return d;
	}

	static Deployment cliDeployment() {
		Deployment d = new Deployment();
		d.setUid( "dpl_cli_001" );
		d.setName( "customer-portal" );
		d.setProjectId( "prj_aaa" );
		d.setUrl( "customer-portal-def.vercel.app" );
		d.setState( "READY" );
		d.setTarget( "production" );
		d.setSource( "cli" );
		d.setChecksConclusion( "succeeded" );
		Deployment.Creator creator = new Deployment.Creator();
		creator.setUid( "user_002" );
		creator.setEmail( "bob@acme.com" );
		d.setCreator( creator );
		d.setCreated( 1_711_000_000_000L );
		return d;
	}

	static Deployment failedDeployment() {
		Deployment d = new Deployment();
		d.setUid( "dpl_fail_001" );
		d.setName( "internal-tools" );
		d.setProjectId( "prj_bbb" );
		d.setState( "ERROR" );
		d.setTarget( "preview" );
		d.setSource( "git" );
		d.setChecksConclusion( "failed" );
		d.setErrorCode( "BUILD_FAILED" );
		Deployment.Creator creator = new Deployment.Creator();
		creator.setUid( "user_003" );
		creator.setEmail( "charlie@acme.com" );
		d.setCreator( creator );
		d.setCreated( 1_711_500_000_000L );
		return d;
	}

	// -------------------------------------------------------------------------
	// Domain fixtures
	// -------------------------------------------------------------------------

	static Domain verifiedDomain() {
		Domain d = new Domain();
		d.setId( "dom_001" );
		d.setName( "acme.com" );
		d.setVerified( true );
		d.setServiceType( "external" );
		d.setTeamId( "team_aaaaaa" );
		d.setRenew( true );
		d.setExpiresAt( 1_800_000_000_000L );
		d.setCreatedAt( 1_600_000_000_000L );
		return d;
	}

	static Domain unverifiedDomain() {
		Domain d = new Domain();
		d.setId( "dom_002" );
		d.setName( "unverified.example.com" );
		d.setVerified( false );
		d.setServiceType( "zeit.world" ); // Vercel-managed DNS
		d.setTeamId( "team_aaaaaa" );
		d.setCreatedAt( 1_710_000_000_000L );
		return d;
	}

	static Domain expiringDomain() {
		Domain d = new Domain();
		d.setId( "dom_003" );
		d.setName( "expiring.example.com" );
		d.setVerified( true );
		d.setServiceType( "external" );
		d.setTeamId( "team_aaaaaa" );
		d.setRenew( false );
		d.setExpiresAt( 1_714_000_000_000L ); // near-future expiry
		d.setCreatedAt( 1_650_000_000_000L );
		return d;
	}

	// -------------------------------------------------------------------------
	// ProjectMember fixtures
	// -------------------------------------------------------------------------

	static ProjectMember projectAdmin() {
		ProjectMember m = new ProjectMember();
		m.setUid( "user_004" );
		m.setEmail( "dave@acme.com" );
		m.setName( "Dave" );
		m.setUsername( "dave" );
		m.setRole( "ADMIN" );
		m.setTeamRole( "DEVELOPER" ); // elevated project role vs team role
		m.setProjectId( "prj_aaa" );
		m.setCreatedAt( 1_690_000_000_000L );
		return m;
	}

	static ProjectMember projectViewer() {
		ProjectMember m = new ProjectMember();
		m.setUid( "user_005" );
		m.setEmail( "eve@acme.com" );
		m.setName( "Eve" );
		m.setUsername( "eve" );
		m.setRole( "PROJECT_VIEWER" );
		m.setTeamRole( "VIEWER" );
		m.setProjectId( "prj_aaa" );
		m.setCreatedAt( 1_695_000_000_000L );
		return m;
	}

	// -------------------------------------------------------------------------
	// Certificate fixtures
	// -------------------------------------------------------------------------

	static Certificate validCertificate() {
		Certificate c = new Certificate();
		c.setId( "cert_001" );
		c.setCns( List.of( "acme.com", "www.acme.com" ) );
		c.setAutoRenew( true );
		c.setExpiresAt( 1_800_000_000_000L ); // far future
		c.setCreatedAt( 1_680_000_000_000L );
		return c;
	}

	static Certificate expiringCertificate() {
		Certificate c = new Certificate();
		c.setId( "cert_002" );
		c.setCns( List.of( "legacy.acme.com" ) );
		c.setAutoRenew( false );
		c.setExpiresAt( 1_713_000_000_000L ); // near-future expiry
		c.setCreatedAt( 1_650_000_000_000L );
		return c;
	}

	// -------------------------------------------------------------------------
	// AccessGroupMember fixtures
	// -------------------------------------------------------------------------

	static AccessGroupMember accessGroupAdminMember() {
		AccessGroupMember m = new AccessGroupMember();
		m.setUid( "user_006" );
		m.setEmail( "eve@acme.com" );
		m.setName( "Eve" );
		m.setUsername( "eve" );
		m.setRole( "ADMIN" );
		m.setTeamRole( "DEVELOPER" );
		m.setAccessGroupId( "ag_111" );
		m.setCreatedAt( 1_690_000_000_000L );
		return m;
	}

	static AccessGroupMember accessGroupViewerMember() {
		AccessGroupMember m = new AccessGroupMember();
		m.setUid( "user_007" );
		m.setEmail( "frank@acme.com" );
		m.setName( "Frank" );
		m.setUsername( "frank" );
		m.setRole( "PROJECT_VIEWER" );
		m.setTeamRole( "VIEWER" );
		m.setAccessGroupId( "ag_111" );
		m.setCreatedAt( 1_695_000_000_000L );
		return m;
	}
}
