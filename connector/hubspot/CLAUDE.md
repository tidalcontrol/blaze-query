## Integration Summary

### What This Connector Does

The HubSpot connector (`blaze-query-connector-hubspot`) exposes HubSpot portal data as queryable SQL tables via the blaze-query connector pattern. It is built using `java.net.http.HttpClient` + Jackson — there is no official HubSpot Java SDK, so no SDK dependency is introduced.

All data is fetched at query time and cached in the `QuerySession`. The connector supports querying across multiple HubSpot portals simultaneously by registering multiple `HubspotClient` instances.

---

### Configuration

**Gradle dependency:**
```groovy
implementation project(':blaze-query-connector-hubspot')
```

**Required setup — one client per portal:**
```java
HubspotClient client = new HubspotClient("your-private-app-access-token");
queryContextBuilder.setProperty(HubspotConnectorConfig.HUBSPOT_CLIENT.getPropertyName(), client);
```

Use a [HubSpot Private App](https://developers.hubspot.com/docs/api/private-apps) token, not an OAuth token. The token is passed as a `Bearer` header on every request.

**Optional — time window for activity fetchers (audit logs, login, security):**
```java
queryContextBuilder.setProperty(
    HubspotConnectorConfig.AUDIT_LOGS_MAX_AGE.getPropertyName(),
    Duration.ofHours(72)
);
```
Defaults to `Duration.ofHours(24)` when not set.

**Required OAuth scopes by fetcher:**

| Fetcher | Scope(s) required |
|---|---|
| Users, Roles, Teams | `settings.users.read` |
| Owners | `crm.objects.owners.read` |
| Account Info | `oauth` |
| Subscription Definitions | `communication_preferences.read` |
| Audit Logs, Login Activity, Security Activity | `account-info.security.read` + **Enterprise subscription** |

---

### Queryable Types and Key Fields

#### `HubspotUser` — portal users (`/settings/v3/users`)

| Field | Type | Notes |
|---|---|---|
| `id` | String | Portal user ID |
| `email` | String | Join with `HubspotOwner.email` to cross-reference CRM access |
| `roleIds` | List\<String\> | Join with `HubspotRole.id` to resolve permissions |
| `primaryTeamId` | String | |
| `superAdmin` | Boolean | Filter `superAdmin = true` to audit privileged accounts |
| `status` | String | `ACTIVE` or `INACTIVE` |
| `createdAt` | OffsetDateTime | |
| `updatedAt` | OffsetDateTime | Use to detect stale/inactive accounts |

#### `HubspotRole` — permission roles (`/settings/v3/users/roles`)

| Field | Type | Notes |
|---|---|---|
| `id` | String | Join with `HubspotUser.roleIds` |
| `name` | String | |
| `requiresBillingWrite` | Boolean | |

#### `HubspotOwner` — CRM record owners (`/crm/v3/owners`)

| Field | Type | Notes |
|---|---|---|
| `id` | String | CRM owner ID |
| `userId` | Integer | Matches `HubspotUser.id` (as integer) |
| `email` | String | Join with `HubspotUser.email` |
| `firstName`, `lastName` | String | |
| `archived` | Boolean | `false` = currently active CRM owner |
| `createdAt`, `updatedAt` | OffsetDateTime | |

#### `HubspotTeam` — org teams (`/settings/v3/users/teams`)

| Field | Type | Notes |
|---|---|---|
| `id` | String | |
| `name` | String | |
| `userIds` | List\<String\> | Primary members |
| `secondaryUserIds` | List\<String\> | Secondary members |
| `parentTeamId` | String | `NULL` for top-level teams. Nested teams require Enterprise |

#### `HubspotAccountInfo` — portal details (`/account-info/v3/details`)

| Field | Type | Notes |
|---|---|---|
| `portalId` | Long | |
| `dataHostingLocation` | String | `na1` = North America, `eu1` = EU — key for GDPR data-residency |
| `accountType` | String | `STANDARD`, `SANDBOX`, `DEVELOPER_TEST`, `LEGACY_DEVELOPER` |
| `timeZone` | String | IANA identifier |
| `currency` | String | ISO-4217 |

#### `HubspotSubscriptionDefinition` — GDPR consent categories (`/communication-preferences/v3/definitions`)

| Field | Type | JSON field | Notes |
|---|---|---|---|
| `id` | String | `id` | |
| `name` | String | `name` | |
| `active` | Boolean | `isActive` | Whether the type is in use |
| `defaultOptIn` | Boolean | `isDefault` | `true` = contacts silently opted in — GDPR risk |
| `internal` | Boolean | `isInternal` | Hidden from contact preference pages |
| `communicationMethod` | String | | `EMAIL`, `WHATSAPP`, etc. |
| `purpose` | String | | Intended use (marketing, transactional, …) |

> **Note:** The record component names (`active`, `defaultOptIn`, `internal`) differ from the JSON field names (`isActive`, `isDefault`, `isInternal`). This is intentional — Calcite treats field names starting with `is` as the `IS` keyword and fails to parse queries. Use the component names in SQL.

#### `HubspotAuditLog` — audit events (`/account-info/v3/activity/audit-logs`) — Enterprise only

| Field | Type | Notes |
|---|---|---|
| `id` | String | |
| `category` | String | `CONTACT`, `DEAL`, `USER`, `INTEGRATION`, … |
| `subCategory` | String | `CREATED`, `UPDATED`, `DELETED`, `PUBLISHED`, … |
| `action` | String | Human-readable description |
| `targetObjectId` | String | ID of the affected object |
| `targetObjectType` | String | |
| `occurredAt` | String | ISO-8601 timestamp |
| `actingUser.userId` | String | Nested — actor's user ID |
| `actingUser.userEmail` | String | Nested — actor's email |

#### `HubspotLoginActivity` — login events (`/account-info/v3/activity/login`) — Enterprise only

| Field | Type | Notes |
|---|---|---|
| `id` | String | |
| `userId` | String | |
| `userEmail` | String | |
| `occurredAt` | String | ISO-8601 |
| `loginType` | String | `WEB`, `MOBILE_APP`, `API` |
| `loginMethod` | String | `PASSWORD`, `SSO`, `TWO_FACTOR`, `OAUTH` |
| `loginStatus` | String | `SUCCESS` or `FAILURE` |
| `mfaUsed` | Boolean | **Primary 2FA audit field** — `false` on a successful login = no MFA |
| `ssoUsed` | Boolean | `false` on a portal that enforces SSO = policy violation |
| `ipAddress` | String | |
| `countryCode` | String | ISO-3166-1 alpha-2 |
| `regionCode` | String | |
| `browser` | String | |
| `device` | String | `DESKTOP`, `MOBILE` |

#### `HubspotSecurityActivity` — security config changes (`/account-info/v3/activity/security`) — Enterprise only

| Field | Type | Notes |
|---|---|---|
| `id` | String | |
| `eventType` | String | `MFA_ENABLED`, `MFA_DISABLED`, `SSO_CONFIGURED`, `SSO_CHANGED`, `PASSWORD_RESET`, `API_TOKEN_CREATED`, `API_TOKEN_REVOKED`, `USER_ADDED`, `USER_REMOVED`, `PERMISSION_CHANGED`, `IP_WHITELIST_MODIFIED` |
| `occurredAt` | String | ISO-8601 |
| `actingUserId` | String | |
| `actingUserEmail` | String | |
| `affectedUserId` | String | |
| `severity` | String | `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` |
| `details` | String | Additional context from HubSpot |

---

### Security & Compliance Query Focus Points

**User access to CRM contacts**
```sql
-- Users who are active CRM owners (have contact access)
-- Note: HubSpot omits the status field for active users — treat NULL as ACTIVE
SELECT u.id, u.email, u.status, u.superAdmin
FROM HubspotUser u
JOIN HubspotOwner o ON u.email = o.email
WHERE o.archived = false AND (u.status = 'ACTIVE' OR u.status IS NULL)
```

**Stale / inactive users**
```sql
-- Note: HubSpot omits the status field for active users — only INACTIVE users have status set
SELECT id, email, status, updatedAt
FROM HubspotUser
WHERE status = 'INACTIVE'
   OR updatedAt < CURRENT_TIMESTAMP - INTERVAL '90' DAY
```

**Super-admin accounts**
```sql
SELECT id, email, primaryTeamId FROM HubspotUser WHERE superAdmin = true
```

**2FA / MFA compliance — logins without MFA** *(Enterprise only)*
```sql
SELECT userEmail, occurredAt, loginMethod, ipAddress, countryCode
FROM HubspotLoginActivity
WHERE mfaUsed = false AND loginStatus = 'SUCCESS'
```

**MFA disabled events** *(Enterprise only)*
```sql
SELECT actingUserEmail, occurredAt, severity, details
FROM HubspotSecurityActivity
WHERE eventType = 'MFA_DISABLED'
```

**GDPR data-residency**
```sql
SELECT portalId, dataHostingLocation, accountType
FROM HubspotAccountInfo
WHERE dataHostingLocation != 'eu1'
```

**GDPR consent — default opt-in risk**
```sql
SELECT id, name, communicationMethod
FROM HubspotSubscriptionDefinition
WHERE defaultOptIn = true AND active = true
```

---

### Caveats and Limitations

1. **No per-user MFA status field.** The Settings Users API (`/settings/v3/users`) does not expose whether MFA is enabled for a user. The only way to audit per-user 2FA compliance is via `HubspotLoginActivity.mfaUsed` — which requires an Enterprise subscription. For non-Enterprise portals, MFA enforcement must be verified in the HubSpot Security settings UI.

2. **Enterprise-only APIs.** `HubspotAuditLogDataFetcher`, `HubspotLoginActivityDataFetcher`, and `HubspotSecurityActivityDataFetcher` all require an Enterprise HubSpot subscription and the `account-info.security.read` scope. On non-Enterprise portals these fetchers will throw a `DataFetcherException` (HTTP 403). Register only the fetchers your subscription supports, or handle the exception in your integration.

3. **Activity fetcher time window.** Audit log, login, and security activity fetchers default to a 24-hour lookback window. Configure `HubspotConnectorConfig.AUDIT_LOGS_MAX_AGE` to extend this. The HubSpot API enforces a maximum lookback of 90 days.

4. **`isActive` / `isDefault` / `isInternal` field naming.** Apache Calcite parses identifiers starting with `is` as the `IS` SQL keyword, causing parse errors. The `HubspotSubscriptionDefinition` record exposes these as `active`, `defaultOptIn`, and `internal` instead, with `@JsonProperty` annotations to preserve the original JSON field names. Always use the Java record component names in SQL queries.

5. **`timestamp` is a reserved SQL keyword.** Any field named `timestamp` will cause a Calcite parse error. All time fields in this connector use `occurredAt` instead.

6. **Owners API only returns active owners by default.** `listOwners()` always appends `archived=false`. Archived owners are not currently returned. If your use case requires archived owners, this would need a separate fetch or an additional flag.

7. **`HubspotOwner.userId` is `Integer`, `HubspotUser.id` is `String`.** Joining on these requires a cast: `CAST(o.userId AS VARCHAR) = u.id`.

8. **No rate-limit handling.** The `HubspotClient` does not implement retry or backoff for HTTP 429 responses. For high-volume portals or frequent polling, add retry logic in a wrapper around `HubspotClient`.
