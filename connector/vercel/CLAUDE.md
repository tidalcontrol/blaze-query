## Integration Summary

This document is aimed at projects integrating the `blaze-query-connector-vercel-jersey3` module.
It covers what the connector does, every queryable type and its key fields, how to wire it up,
and caveats discovered during development.

---

### What the Connector Does

The Vercel connector exposes Vercel REST API resources as SQL-queryable tables inside a
Blaze-Query `QuerySession`. It is oriented towards **security and compliance** use-cases:
auditing access controls, deployment hygiene, secret handling, domain and certificate health,
firewall posture, and third-party integration permissions.

Data is fetched on-demand when a query first touches a type, then cached for the lifetime of
the `QuerySession`. There is no background polling and no persistent state.

All HTTP calls are made against `https://api.vercel.com` using Jersey 3 + Jackson. The
connector ships a single artifact: `blaze-query-connector-vercel-jersey3`.

---

### Configuration

#### 1. Add the dependency

```gradle
implementation 'com.blazebit:blaze-query-connector-vercel-jersey3:2.4.4'
```

#### 2. Register the schema provider

The provider is auto-discovered via `META-INF/services` when you call
`Queries.createQueryContextBuilder().loadServices()`. For manual registration:

```java
queryContextBuilder.registerSchemaProvider(new VercelSchemaProvider());
```

#### 3. Register schema aliases (once, on the QueryContext builder)

```java
queryContextBuilder.registerSchemaObjectAlias(AuthToken.class,                    "VercelAuthToken");
queryContextBuilder.registerSchemaObjectAlias(Team.class,                          "VercelTeam");
queryContextBuilder.registerSchemaObjectAlias(TeamMember.class,                    "VercelTeamMember");
queryContextBuilder.registerSchemaObjectAlias(AccessGroup.class,                   "VercelAccessGroup");
queryContextBuilder.registerSchemaObjectAlias(AccessGroupMember.class,             "VercelAccessGroupMember");
queryContextBuilder.registerSchemaObjectAlias(Webhook.class,                       "VercelWebhook");
queryContextBuilder.registerSchemaObjectAlias(Project.class,                       "VercelProject");
queryContextBuilder.registerSchemaObjectAlias(ProjectMember.class,                 "VercelProjectMember");
queryContextBuilder.registerSchemaObjectAlias(EnvironmentVariable.class,           "VercelEnvVar");
queryContextBuilder.registerSchemaObjectAlias(LogDrain.class,                      "VercelLogDrain");
queryContextBuilder.registerSchemaObjectAlias(FirewallConfig.class,                "VercelFirewallConfig");
queryContextBuilder.registerSchemaObjectAlias(IntegrationConfiguration.class,      "VercelIntegration");
queryContextBuilder.registerSchemaObjectAlias(Deployment.class,                    "VercelDeployment");
queryContextBuilder.registerSchemaObjectAlias(Domain.class,                        "VercelDomain");
queryContextBuilder.registerSchemaObjectAlias(Certificate.class,                   "VercelCertificate");
```

> **Name collision**: `Project` is a common class name. If your application also imports
> `org.gitlab4j.api.models.Project` or similar, use the fully-qualified class name:
> `com.blazebit.query.connector.vercel.Project.class`.

#### 4. Provide the API client on each session

```java
try (QuerySession session = context.createSession()) {
    session.setProperty(
        VercelConnectorConfig.API_CLIENT.getPropertyName(),
        new VercelApiClient("your-personal-access-token", "team_xxxxxxxx")
    );
    // run queries ...
}
```

`VercelApiClient` constructor:
- `new VercelApiClient(String accessToken)` — user-level; no `teamId` appended
- `new VercelApiClient(String accessToken, String teamId)` — team-scoped; `teamId` is
  automatically appended as a query parameter to every request that supports it

The `teamId` parameter format is `team_xxxxxxxx` (Vercel team ID, not slug).

---

### Queryable Types

#### AuthToken — `VercelAuthToken`

API: `GET /v6/user/tokens`

| Column | Type | Notes |
|---|---|---|
| `id` | String | Token identifier |
| `name` | String | Human-readable name |
| `type` | String | `authentication-token` or `oauth2-token` |
| `origin` | String | `manual`, `saml`, `github`, `gitlab`, `email`, … |
| `scopes` | List | Nested `TokenScope` objects |
| `expiresAt` | Long | Unix ms; null = never expires |
| `activeAt` | Long | Unix ms of last use; null = never used |
| `createdAt` | Long | Unix ms |

`TokenScope` fields: `type`, `teamId`, `origin`, `createdAt`, `expiresAt`

**Security focus**: `activeAt IS NULL` finds stale tokens never used. `expiresAt IS NOT NULL`
finds tokens with a limited lifetime.

---

#### Team — `VercelTeam`

API: `GET /v2/teams` or `GET /v2/teams/{teamId}`

| Column | Type | Notes |
|---|---|---|
| `id` | String | Team identifier |
| `slug` | String | URL slug |
| `name` | String | Display name |
| `creatorId` | String | User ID of creator |
| `saml.enforced` | Boolean | SAML SSO enforced for all members |
| `saml.connection.type` | String | IdP type: `OktaSAML`, `AzureSAML`, … |
| `saml.connection.state` | String | `active`, `inactive`, `error` |
| `saml.directory.state` | String | `SETUP` or `ACTIVE` (SCIM) |
| `saml.directory.lastSyncedAt` | Long | Unix ms of last directory sync |
| `sensitiveEnvironmentVariablePolicy` | String | `on`, `off`, or null |
| `hideIpAddresses` | Boolean | IPs hidden in runtime logs |
| `hideIpAddressesInLogDrains` | Boolean | IPs hidden in log drain output |
| `createdAt` / `updatedAt` | Long | Unix ms |

`defaultDeploymentProtection` is a nested `DeploymentProtection` with child
`passwordProtection` and `ssoProtection` each having a `deploymentType` field.

**Security focus**: `saml.enforced = false OR saml IS NULL` finds teams without mandatory SSO.
`sensitiveEnvironmentVariablePolicy = 'off'` finds teams where plain-text secrets can be set.

---

#### TeamMember — `VercelTeamMember`

API: `GET /v3/teams/{teamId}/members` (fetched per team; depends on `TeamDataFetcher`)

| Column | Type | Notes |
|---|---|---|
| `uid` | String | User identifier |
| `email` | String | |
| `name` | String | |
| `username` | String | |
| `role` | String | `OWNER`, `MEMBER`, `DEVELOPER`, `SECURITY`, `BILLING`, `VIEWER`, `CONTRIBUTOR` |
| `teamId` | String | Set by fetcher |
| `confirmed` | Boolean | false = pending invite not yet accepted |
| `joinedFrom.origin` | String | `dsync`, `saml`, `mail`, `import`, `link`, `teams`, `github` |
| `joinedFrom.samlConnectedAt` | Long | Unix ms |
| `joinedFrom.dsyncConnectedAt` | Long | Unix ms |
| `createdAt` / `updatedAt` | Long | Unix ms |

**Security focus**: `confirmed = false` finds pending members. `joinedFrom.origin <> 'dsync' AND joinedFrom.origin <> 'saml'` finds manually-added members outside automated provisioning.

---

#### AccessGroup — `VercelAccessGroup`

API: `GET /v1/access-groups`

| Column | Type | Notes |
|---|---|---|
| `accessGroupId` | String | Group identifier |
| `name` | String | |
| `teamId` | String | |
| `membersCount` | Integer | |
| `projectsCount` | Integer | |
| `isDsyncManaged` | Boolean | Managed by SCIM directory sync |
| `createdAt` / `updatedAt` | String | ISO-8601 timestamp |

**Security focus**: `isDsyncManaged = false OR isDsyncManaged IS NULL` finds manually-managed groups that bypass automated provisioning.

---

#### AccessGroupMember — `VercelAccessGroupMember`

API: `GET /v1/access-groups/{id}/members` (fetched per group; depends on `AccessGroupDataFetcher`)

| Column | Type | Notes |
|---|---|---|
| `uid` | String | User identifier |
| `email` | String | |
| `name` | String | |
| `username` | String | |
| `role` | String | Access group role: `ADMIN`, `PROJECT_DEVELOPER`, `PROJECT_VIEWER` |
| `teamRole` | String | Team-level role for comparison |
| `accessGroupId` | String | Set by fetcher |
| `createdAt` | Long | Unix ms |

**Security focus**: Compare `role` against `teamRole` to detect privilege escalation —
an `ADMIN` role in the access group for a user whose `teamRole` is `DEVELOPER` indicates
elevated access granted outside normal team permissions.

---

#### Webhook — `VercelWebhook`

API: `GET /v1/webhooks` (bare JSON array)

| Column | Type | Notes |
|---|---|---|
| `id` | String | |
| `url` | String | Endpoint receiving POST events |
| `events` | List | Subscribed event types |
| `ownerId` | String | |
| `teamId` | String | |
| `projectIds` | List | Empty = team-wide scope |
| `createdAt` / `updatedAt` | Long | Unix ms |

Security-critical event types: `firewall.attack`, `firewall.system-rule-anomaly`,
`firewall.custom-rule-anomaly`, `project.env-variable.created/updated/deleted`,
`integration-configuration.permission-upgraded`, `deployment.checks.failed`.

**Security focus**: `CARDINALITY(projectIds) = 0` finds team-wide webhooks with broad exposure.

---

#### Project — `VercelProject`

API: `GET /v9/projects`

| Column | Type | Notes |
|---|---|---|
| `id` | String | |
| `name` | String | |
| `accountId` | String | Owning team or user ID |
| `framework` | String | e.g. `nextjs`, `vite` |
| `passwordProtection` | Object | null = no password protection |
| `passwordProtection.deploymentType` | String | `all_deployments`, `only_preview_deployments`, … |
| `ssoProtection` | Object | null = no SSO protection |
| `ssoProtection.deploymentType` | String | Same values as above |
| `autoExposeSystemEnvs` | Boolean | `true` exposes `VERCEL_URL` etc. |
| `createdAt` / `updatedAt` | Long | Unix ms |

**Security focus**: `passwordProtection IS NULL AND ssoProtection IS NULL` finds entirely
unprotected projects. `autoExposeSystemEnvs = true` surfaces information-disclosure risk.

---

#### ProjectMember — `VercelProjectMember`

API: `GET /v1/projects/{id}/members` (fetched per project; depends on `ProjectDataFetcher`)

| Column | Type | Notes |
|---|---|---|
| `uid` | String | |
| `email` | String | |
| `name` | String | |
| `username` | String | |
| `role` | String | Project role: `ADMIN`, `PROJECT_DEVELOPER`, `PROJECT_VIEWER` |
| `teamRole` | String | Team-level role |
| `projectId` | String | Set by fetcher |
| `createdAt` / `updatedAt` | Long | Unix ms |

**Security focus**: `role = 'ADMIN' AND teamRole <> 'OWNER'` finds users with elevated project
access that exceeds their team-level role — a classic privilege escalation pattern.

---

#### EnvironmentVariable — `VercelEnvVar`

API: `GET /v9/projects/{id}/env` (fetched per project; depends on `ProjectDataFetcher`)

| Column | Type | Notes |
|---|---|---|
| `id` | String | |
| `key` | String | Variable name |
| `type` | String | `plain` (highest risk), `encrypted`, `sensitive`, `secret`, `system` |
| `target` | List | `production`, `preview`, `development` |
| `gitBranch` | String | Branch scope; null = all branches |
| `projectId` | String | Set by fetcher |
| `systemGenerated` | Boolean | Auto-generated system variable; null = user-managed |
| `comment` | String | Description |
| `createdAt` / `updatedAt` | Long | Unix ms |
| `createdBy` / `updatedBy` | String | User IDs |

> **Important**: The Vercel API redacts the actual value for `encrypted` and `sensitive`
> variables. Only `key`, `type`, `target`, and metadata are available.

> **Caveat**: The `system` field in the Vercel API response maps to the Java field
> `systemGenerated` (renamed to avoid a collision with the SQL reserved word `SYSTEM`).
> The `@JsonProperty("system")` annotation handles deserialization transparently.
> Use `e.systemGenerated` in queries, not `e.system`.

**Security focus**: `type = 'plain'` finds variables stored as plain text (risk of accidental
secret exposure). `CARDINALITY(target) = 1` finds single-environment variables likely scoped
exclusively to production. `systemGenerated IS NULL` finds all user-managed variables.

---

#### LogDrain — `VercelLogDrain`

API: `GET /v1/log-drains` (bare JSON array)

| Column | Type | Notes |
|---|---|---|
| `id` | String | |
| `name` | String | |
| `url` | String | Receiving endpoint |
| `deliveryFormat` | String | `json`, `ndjson`, `syslog` |
| `sources` | List | `build`, `edge`, `lambda`, `request`, `external`, `firewall` |
| `environments` | List | `production`, `preview` |
| `projectIds` | List | Empty = team-wide |
| `teamId` | String | |
| `clientId` | String | OAuth client if installed via integration |
| `configurationId` | String | Integration configuration ID |
| `createdAt` | Long | Unix ms |

**Security focus**: Absence of any log drain (`CARDINALITY` of all results = 0) means no audit
trail. `CARDINALITY(projectIds) = 0` finds team-wide drains. `CARDINALITY(sources) = 1` is a
proxy for drains that likely miss `request`-type HTTP access logs (since broad SIEM drains
typically include multiple sources).

---

#### FirewallConfig — `VercelFirewallConfig`

API: `GET /v1/security/firewall/config/latest?projectIdOrName={id}` (fetched per project;
depends on `ProjectDataFetcher`)

| Column | Type | Notes |
|---|---|---|
| `id` | String | |
| `ownerId` | String | |
| `projectKey` | String | Project name/slug |
| `projectId` | String | Set by fetcher |
| `firewallEnabled` | Boolean | WAF enabled |
| `botIdEnabled` | Boolean | Bot detection enabled |
| `managedRules.owaspCoruleSet.active` | Boolean | OWASP CRS active |
| `managedRules.owaspCoruleSet.action` | String | `deny` or `log` |
| `managedRules.botProtection.active` | Boolean | |
| `managedRules.vercelRuleset.active` | Boolean | |
| `managedRules.aiBots.active` | Boolean | |
| `rules` | List | Custom `FirewallRule` objects |
| `ips` | List | IP block/allow `IpRule` objects |
| `updatedAt` | String | |

`FirewallRule` fields: `id`, `name`, `active`, `action` (`deny`, `log`, `challenge`, `bypass`)
`IpRule` fields: `id`, `hostname`, `ip`, `notes`, `action` (`deny`, `allow`)

> **Caveat**: The firewall config endpoint is only available on Vercel Pro/Enterprise plans.
> `FirewallConfigDataFetcher` **silently swallows per-project exceptions** — if a project is on
> a plan without WAF access, it is simply omitted from results rather than causing a failure.
> An absent row for a project does not definitively mean WAF is disabled; it may mean the
> project plan does not support it.

**Security focus**: `firewallEnabled = false OR firewallEnabled IS NULL` finds projects with WAF
disabled. `botIdEnabled = false OR botIdEnabled IS NULL` finds projects without bot protection.
Check `managedRules.owaspCoruleSet.active` for OWASP CRS enforcement.

---

#### IntegrationConfiguration — `VercelIntegration`

API: `GET /v1/integrations/configurations` (bare JSON array)

| Column | Type | Notes |
|---|---|---|
| `id` | String | |
| `slug` | String | Integration name, e.g. `datadog`, `sentry` |
| `integrationId` | String | |
| `ownerId` / `teamId` / `userId` | String | |
| `status` | String | `ready`, `pending`, `error`, `suspended`, `uninstalled` |
| `source` | String | `marketplace`, `external`, `oauth`, `cli`, … |
| `installationType` | String | |
| `scopes` | List | OAuth scopes granted |
| `projects` | List | Project IDs; empty = access to all projects |
| `createdAt` / `updatedAt` | Long | Unix ms |
| `disabledAt` | Long | Unix ms; null = currently enabled |
| `disabledReason` | String | |
| `deletedAt` | Long | Unix ms; null = still installed |

**Security focus**: `status = 'suspended' OR disabledAt IS NOT NULL` finds disabled integrations
that may still hold valid OAuth tokens. `CARDINALITY(projects) = 0` finds team-wide integrations
with broad access. Broad `scopes` (e.g. `write:env`) warrant extra scrutiny.

---

#### Deployment — `VercelDeployment`

API: `GET /v6/deployments`

| Column | Type | Notes |
|---|---|---|
| `uid` | String | Deployment identifier |
| `name` | String | Project name at time of deploy |
| `projectId` | String | |
| `url` | String | Deployment URL |
| `state` | String | `BUILDING`, `READY`, `ERROR`, `QUEUED`, `CANCELED`, `DELETED` |
| `target` | String | `production`, `staging`, or null for preview |
| `source` | String | `git`, `cli`, `api-trigger-git-deploy`, `import`, `redeploy`, … |
| `checksConclusion` | String | `succeeded`, `failed`, `skipped`, `canceled` |
| `errorCode` | String | Error code on failure |
| `errorMessage` | String | |
| `creator.uid` | String | |
| `creator.email` | String | |
| `creator.username` | String | |
| `created` | Long | Unix ms |
| `buildingAt` / `ready` | Long | Unix ms |

**Security focus**: `target = 'production' AND source <> 'git'` finds deployments that bypassed
the standard git-based CI/CD review pipeline (CLI push, API trigger, etc.).
`state = 'ERROR'` combined with `errorCode` identifies broken production deployments.
`checksConclusion = 'failed'` finds deployments where security checks did not pass.

---

#### Domain — `VercelDomain`

API: `GET /v5/domains`

| Column | Type | Notes |
|---|---|---|
| `id` | String | |
| `name` | String | Domain name |
| `verified` | Boolean | Ownership verified; false = misconfiguration or takeover risk |
| `serviceType` | String | `zeit.world` (Vercel DNS), `external` (CNAME/A), `na` |
| `teamId` / `userId` | String | |
| `renew` | Boolean | Auto-renew enabled |
| `expiresAt` | Long | Unix ms; null for pure external DNS |
| `boughtAt` | Long | Unix ms; null if externally owned |
| `transferredAt` | Long | Unix ms; null if never transferred |
| `createdAt` | Long | Unix ms |

**Security focus**: `verified = false OR verified IS NULL` finds domains at risk of
misconfiguration or subdomain takeover. `renew = false AND expiresAt IS NOT NULL` finds domains
that will expire and are at hijack risk. Filter `serviceType = 'external'` to focus on domains
using external DNS where Vercel has less control.

---

#### Certificate — `VercelCertificate`

API: `GET /v7/certs`

| Column | Type | Notes |
|---|---|---|
| `id` | String | |
| `cns` | List | Common names (domains) covered |
| `autoRenew` | Boolean | Vercel auto-renews before expiry |
| `expiresAt` | Long | Unix ms |
| `createdAt` | Long | Unix ms |

**Security focus**: `autoRenew = false OR autoRenew IS NULL` finds certificates requiring manual
renewal. Cross-reference with `expiresAt` to prioritise near-term risk.

---

### Fetcher Dependency Graph

Some fetchers call `session.getOrFetch()` to reuse data already retrieved in the same session.
This means query order can affect which API calls are made.

```
TeamDataFetcher
    └── TeamMemberDataFetcher          (depends on Team)

AccessGroupDataFetcher
    └── AccessGroupMemberDataFetcher   (depends on AccessGroup)

ProjectDataFetcher
    ├── ProjectMemberDataFetcher       (depends on Project)
    ├── EnvironmentVariableDataFetcher (depends on Project)
    └── FirewallConfigDataFetcher      (depends on Project)

Independent (no cross-fetcher deps):
    AuthTokenDataFetcher
    WebhookDataFetcher
    LogDrainDataFetcher
    IntegrationConfigurationDataFetcher
    DeploymentDataFetcher
    DomainDataFetcher
    CertificateDataFetcher
```

When you query `VercelTeamMember`, the engine will automatically fetch `VercelTeam` first if
teams have not yet been fetched in the current session.

---

### Caveats and Limitations

1. **`EnvironmentVariable.systemGenerated` vs `system`**
   The Vercel API field is named `system`. This was renamed to `systemGenerated` in the Java
   model because `SYSTEM` is a reserved word in Calcite SQL (the query engine used by
   Blaze-Query) and causes a parse error. The `@JsonProperty("system")` annotation ensures
   correct JSON deserialization. Always use `e.systemGenerated` in SQL queries.

2. **`MEMBER OF` is not supported**
   Calcite's SQL dialect does not support the `MEMBER OF` operator for list containment.
   Use `CARDINALITY(list) = N` as a proxy for list-size checks. There is currently no direct
   SQL equivalent for "does list contain value X" in this dialect.

3. **`!=` is not supported — use `<>`**
   The Calcite dialect in use rejects `!=`. Always use `<>` for inequality comparisons.

4. **FirewallConfig silently drops unavailable projects**
   `FirewallConfigDataFetcher` wraps each per-project API call in a try-catch and silently
   skips projects that return errors. This is intentional — the WAF endpoint is only available
   on Pro/Enterprise plans. A missing row for a project does **not** definitively mean WAF is
   disabled; it may mean the project's plan does not support it. Do not rely on
   `CARDINALITY(VercelFirewallConfig) = CARDINALITY(VercelProject)` as a compliance check.

5. **Environment variable values are redacted**
   The Vercel API does not return the actual value of `encrypted` and `sensitive` variables.
   Only `key`, `type`, `target`, and metadata fields are populated. `plain` variables may
   return a value in some API responses but this should not be relied upon.

6. **Deployments are limited to the most recent results**
   `GET /v6/deployments` returns deployments in reverse chronological order. The fetcher
   paginates using the `until` cursor but Vercel enforces server-side limits on deployment
   history depth. Deep historical queries may not return all deployments.

7. **`AuthToken` is user-scoped, not team-scoped**
   `AuthTokenDataFetcher` calls `/v6/user/tokens`, which always returns tokens for the
   authenticated user regardless of the `teamId` configured on the client. The `teamId`
   query parameter is not sent for this endpoint.

8. **Bare-array endpoints have no `itemsKey`**
   Webhooks, Log Drains, and Integration Configurations return a bare JSON array (no wrapper
   object). These are handled internally by passing `null` as `itemsKey` to `fetchPagedList`.
   This is transparent to callers but useful to know when interpreting API documentation.

9. **`system` as a SQL reserved word**
   Beyond `EnvironmentVariable.systemGenerated` (point 1), be careful naming any future model
   field with SQL reserved words. Other known problematic names: `value`, `key`, `table`,
   `order`, `group`. When in doubt, add a `@JsonProperty` mapping to a safe Java field name.

---

### Example Security & Compliance Queries

```sql
-- Auth tokens never used (stale credentials)
SELECT t.id, t.name, t.origin, t.createdAt
FROM VercelAuthToken t
WHERE t.activeAt IS NULL

-- Teams without SAML enforcement
SELECT t.id, t.slug
FROM VercelTeam t
WHERE t.saml.enforced = false OR t.saml IS NULL

-- Unconfirmed (pending) team members
SELECT m.uid, m.email, m.teamId
FROM VercelTeamMember m
WHERE m.confirmed = false

-- Members joined outside automated provisioning
SELECT m.uid, m.email, m.role, m.teamId
FROM VercelTeamMember m
WHERE m.joinedFrom.origin <> 'dsync' AND m.joinedFrom.origin <> 'saml'

-- Privilege escalation: project ADMIN whose team role is not OWNER
SELECT m.uid, m.email, m.role, m.teamRole, m.projectId
FROM VercelProjectMember m
WHERE m.role = 'ADMIN' AND m.teamRole <> 'OWNER'

-- Projects with no deployment protection at all
SELECT p.id, p.name
FROM VercelProject p
WHERE p.passwordProtection IS NULL AND p.ssoProtection IS NULL

-- Plain-text environment variables (secret exposure risk)
SELECT e.id, e.key, e.projectId
FROM VercelEnvVar e
WHERE e.type = 'plain'

-- Non-git production deployments (bypass CI/CD review)
SELECT d.uid, d.name, d.source, d.creator.email
FROM VercelDeployment d
WHERE d.target = 'production' AND d.source <> 'git'

-- WAF disabled on projects
SELECT f.id, f.projectKey
FROM VercelFirewallConfig f
WHERE f.firewallEnabled = false OR f.firewallEnabled IS NULL

-- Domains unverified (takeover risk)
SELECT d.id, d.name
FROM VercelDomain d
WHERE d.verified = false OR d.verified IS NULL

-- Domains with auto-renew disabled and a known expiry (hijack risk)
SELECT d.id, d.name, d.expiresAt
FROM VercelDomain d
WHERE d.renew = false AND d.expiresAt IS NOT NULL

-- Certificates without auto-renew
SELECT c.id, c.expiresAt
FROM VercelCertificate c
WHERE c.autoRenew = false OR c.autoRenew IS NULL

-- Suspended or disabled integrations (may still hold live OAuth tokens)
SELECT i.id, i.slug, i.status, i.disabledAt
FROM VercelIntegration i
WHERE i.status = 'suspended' OR i.disabledAt IS NOT NULL

-- Team-wide log drains (confirm audit trail coverage)
SELECT d.id, d.name, d.url
FROM VercelLogDrain d
WHERE CARDINALITY(d.projectIds) = 0

-- Access group members with elevated group role vs team role
SELECT m.uid, m.email, m.role, m.teamRole, m.accessGroupId
FROM VercelAccessGroupMember m
WHERE m.role = 'ADMIN' AND m.teamRole <> 'OWNER'
```
