## Integration Summary

This document describes the `blaze-query-connector-scaleway` module for teams building on top of it.
It covers what the connector does, every queryable type and its fields, how to configure it, and
caveats discovered during development.

### What the connector does

The Scaleway connector lets you run SQL queries against Scaleway cloud infrastructure using
Apache Calcite's SQL engine. Data is fetched live from the Scaleway REST API and cached
in-memory for the lifetime of a `QuerySession`. There is no official Scaleway Java SDK — all
HTTP calls are made with Java 17's built-in `HttpClient`.

The connector covers **29 resource types** across IAM, Compute, Object Storage, Managed
Databases, Secret Manager, KMS, Serverless, Block Storage, Load Balancers, Networking,
Audit Trail, Kubernetes, Container Registry, Observability, and Transactional Email.

---

### Configuration

**1. Build dependency**

```gradle
implementation project(':blaze-query-connector-scaleway')
```

**2. Construct a `ScalewayClient`**

```java
ScalewayClient client = new ScalewayClient(
    "your-secret-key",           // Scaleway API secret key (X-Auth-Token)
    "your-organization-id",      // Scaleway organization ID
    List.of("fr-par-1", "fr-par-2", "nl-ams-1", "pl-waw-1")  // zones to query
);
```

Zones drive all zone-based APIs (Compute, Block Storage, Load Balancers, Flexible IPs).
Regions are automatically derived from zones by stripping the trailing zone index
(`fr-par-1` → `fr-par`). All region-based APIs (Secret Manager, KMS, Kubernetes, etc.)
will query each unique derived region.

**3. Register the connector**

```java
QueryContext ctx = Queries.createQueryContextBuilder()
    .loadServices()
    .setProperty(ScalewayConnectorConfig.SCALEWAY_CLIENT, client)
    // register aliases per type you want to query:
    .registerSchemaObjectAlias(ScalewayIamUser.class, "ScalewayIamUser")
    // ... repeat for each type
    .build();
```

**4. `ScalewayConnectorConfig`**

```java
// The single config key — supply a ScalewayClient instance
ScalewayConnectorConfig.SCALEWAY_CLIENT
```

> **Note:** There is a typo in the property name (`"scaewayClient"` — missing an `l`).
> This is the internal property key used by `DataFetcherConfig.forPropertyName()` and is
> not user-visible; always use `ScalewayConnectorConfig.SCALEWAY_CLIENT` as the key constant.

---

### Queryable types and fields

All timestamp fields (`createdAt`, `updatedAt`, `expiresAt`, etc.) are ISO-8601 strings,
not `java.time` objects. Boolean fields never use the `is` prefix (a Calcite constraint —
see Caveats below).

#### IAM

**`ScalewayIamUser`**
| Field | Type | Notes |
|---|---|---|
| `id` | String | |
| `email` | String | |
| `organizationId` | String | |
| `status` | String | `invitation_pending`, `activated` |
| `mfa` | boolean | `true` = MFA enrolled |
| `type` | String | `unknown_type`, `guest`, `owner` |
| `createdAt` | String | |
| `updatedAt` | String | |
| `lastLoginAt` | String | nullable |

Security focus: `WHERE mfa = false` finds users without MFA — a common CIS finding.

**`ScalewayIamGroup`**
| Field | Type | Notes |
|---|---|---|
| `id` | String | |
| `name` | String | |
| `description` | String | |
| `organizationId` | String | |
| `userIds` | List\<String\> | member user IDs |
| `applicationIds` | List\<String\> | member application IDs |
| `createdAt` | String | |
| `updatedAt` | String | |

**`ScalewayIamApplication`** (service accounts)
| Field | Type | Notes |
|---|---|---|
| `id` | String | |
| `name` | String | |
| `description` | String | |
| `organizationId` | String | |
| `editable` | boolean | |
| `nbApiKeys` | int | number of API keys attached |
| `createdAt` | String | |
| `updatedAt` | String | |

Security focus: `WHERE nbApiKeys = 0` finds unused service accounts.

**`ScalewayIamApiKey`**
| Field | Type | Notes |
|---|---|---|
| `accessKey` | String | key ID (not the secret — never exposed by API) |
| `applicationId` | String | nullable — set if owned by an application |
| `userId` | String | nullable — set if owned by a user |
| `description` | String | |
| `editable` | boolean | |
| `createdAt` | String | |
| `expiresAt` | String | nullable — `NULL` = never expires |
| `defaultProjectId` | String | |

Security focus: `WHERE expiresAt IS NULL` finds non-expiring keys. Note: `accessKey` is
the primary identifier, not `id` (there is no `id` field).

**`ScalewayIamPolicy`**
| Field | Type | Notes |
|---|---|---|
| `id` | String | |
| `name` | String | |
| `description` | String | |
| `organizationId` | String | |
| `userId` | String | nullable |
| `groupId` | String | nullable |
| `applicationId` | String | nullable |
| `editable` | boolean | |
| `nbRules` | int | number of rules |
| `nbScopes` | int | blast-radius indicator |
| `nbPermissionSets` | int | blast-radius indicator |
| `createdAt` | String | |
| `updatedAt` | String | |

Security focus: high `nbPermissionSets` or `nbScopes` signals an overly broad policy.

**`ScalewayIamSshKey`**
| Field | Type | Notes |
|---|---|---|
| `id` | String | |
| `name` | String | |
| `publicKey` | String | full public key material |
| `fingerprint` | String | |
| `disabled` | boolean | |
| `organizationId` | String | |
| `projectId` | String | |
| `createdAt` | String | |
| `updatedAt` | String | |

Security focus: `WHERE disabled = true` finds deactivated but not deleted keys.

---

#### Compute

**`ScalewayInstance`**
| Field | Type | Notes |
|---|---|---|
| `id` | String | |
| `name` | String | |
| `state` | String | `running`, `stopped`, `stopped in place`, etc. |
| `commercialType` | String | e.g., `DEV1-S`, `GP1-M` |
| `arch` | String | `x86_64`, `arm64` |
| `hostname` | String | |
| `privateIp` | String | nullable |
| `publicIp` | String | nullable — outer `public_ip.address` |
| `enableIpv6` | boolean | |
| `dynamicIpRequired` | boolean | |
| `instanceProtected` | boolean | deletion protection enabled |
| `tags` | List\<String\> | |
| `zone` | String | |
| `organizationId` | String | |
| `projectId` | String | |
| `imageId` | String | |
| `imageName` | String | |
| `securityGroupId` | String | |
| `securityGroupName` | String | |
| `createdAt` | String | |

Security focus: `WHERE publicIp IS NOT NULL AND instanceProtected = false` finds
internet-exposed instances without deletion protection.

**`ScalewaySecurityGroup`**
| Field | Type | Notes |
|---|---|---|
| `id` | String | |
| `name` | String | |
| `description` | String | |
| `zone` | String | |
| `organizationId` | String | |
| `projectId` | String | |
| `inboundDefaultPolicy` | String | `accept` or `drop` |
| `outboundDefaultPolicy` | String | `accept` or `drop` |
| `stateful` | boolean | |
| `enableDefaultSecurity` | boolean | |
| `organizationDefault` | boolean | |
| `projectDefault` | boolean | |
| `createdAt` | String | |
| `modifiedAt` | String | |

Security focus: `WHERE inboundDefaultPolicy = 'accept'` is a critical misconfiguration —
all inbound traffic is allowed unless explicitly blocked.

**`ScalewaySecurityGroupRule`**
| Field | Type | Notes |
|---|---|---|
| `id` | String | |
| `securityGroupId` | String | parent group |
| `zone` | String | |
| `position` | int | rule evaluation order |
| `protocol` | String | `TCP`, `UDP`, `ICMP`, `ANY` |
| `direction` | String | `inbound`, `outbound` |
| `action` | String | `accept`, `drop` |
| `ipRange` | String | CIDR, e.g., `0.0.0.0/0` |
| `destPortFrom` | Integer | nullable (null = all ports) |
| `destPortTo` | Integer | nullable |
| `editable` | boolean | |

Security focus: `WHERE ipRange = '0.0.0.0/0' AND direction = 'inbound' AND action = 'accept'`
finds rules exposing services to the entire internet.

---

#### Object Storage

**`ScalewayObjectStorageBucket`**
| Field | Type | Notes |
|---|---|---|
| `name` | String | bucket name (acts as unique ID) |
| `region` | String | |
| `projectId` | String | |
| `organizationId` | String | |
| `objectCount` | long | number of objects |
| `totalSize` | long | total bytes |
| `tags` | List\<String\> | |
| `createdAt` | String | |
| `updatedAt` | String | |

Note: The Object Storage API does not expose bucket ACL/policy or versioning status in the
list endpoint. Public access controls must be checked via separate per-bucket calls not
yet implemented. Use `objectCount = 0` to find empty/orphaned buckets.

---

#### Managed Databases (RDB)

**`ScalewayDatabase`**
| Field | Type | Notes |
|---|---|---|
| `id` | String | |
| `name` | String | |
| `status` | String | `ready`, `creating`, `error`, etc. |
| `engine` | String | e.g., `PostgreSQL-15`, `MySQL-8` |
| `volumeType` | String | `bssd`, `lssd` |
| `region` | String | |
| `projectId` | String | |
| `organizationId` | String | |
| `haEnabled` | boolean | high-availability cluster |
| `publiclyAccessible` | boolean | endpoint reachable from internet |
| `backupRetentionDays` | int | 0 = not configured |
| `endpointIp` | String | nullable |
| `tags` | List\<String\> | |
| `createdAt` | String | |
| `updatedAt` | String | |

Security focus: `WHERE publiclyAccessible = true` is the highest-priority finding.
`WHERE backupRetentionDays < 7` catches insufficient backup retention (SOC 2 / ISO 27001).
`WHERE haEnabled = false` identifies single-point-of-failure databases.

---

#### Secret Manager

**`ScalewaySecret`**
| Field | Type | Notes |
|---|---|---|
| `id` | String | |
| `name` | String | |
| `description` | String | |
| `status` | String | `ready`, `locked` |
| `region` | String | |
| `projectId` | String | |
| `organizationId` | String | |
| `versionCount` | int | 0 = orphaned secret |
| `tags` | List\<String\> | |
| `createdAt` | String | |
| `updatedAt` | String | |

**`ScalewaySecretVersion`**
| Field | Type | Notes |
|---|---|---|
| `secretId` | String | parent secret |
| `revision` | String | version number as string |
| `status` | String | `enabled`, `disabled` |
| `region` | String | |
| `createdAt` | String | |
| `updatedAt` | String | |

Note: `ScalewaySecretVersion` is a dependent fetcher — it requires `ScalewaySecret` to be
fetched first. When using `session.put()` in tests, put secrets before versions, or put
versions directly (the fetcher is skipped when data is manually supplied).

---

#### KMS

**`ScalewayKmsKey`**
| Field | Type | Notes |
|---|---|---|
| `id` | String | |
| `name` | String | |
| `description` | String | |
| `state` | String | `enabled`, `disabled`, `pending_key_material` |
| `algorithm` | String | e.g., `aes_256_gcm`, `rsa_4096_oaep_sha256` |
| `region` | String | |
| `projectId` | String | |
| `organizationId` | String | |
| `rotationPeriod` | String | nullable — ISO-8601 duration |
| `nextRotationAt` | String | nullable |
| `lastRotatedAt` | String | nullable |
| `rotationEnabled` | boolean | derived: `true` if `rotationPeriod` is set |
| `createdAt` | String | |
| `updatedAt` | String | |

Security focus: `WHERE rotationEnabled = false AND state = 'enabled'` finds active keys
with no rotation policy.

---

#### Serverless

**`ScalewayContainer`**
| Field | Type | Notes |
|---|---|---|
| `id` | String | |
| `name` | String | |
| `namespaceId` | String | |
| `status` | String | `ready`, `pending`, `error`, etc. |
| `region` | String | |
| `privacy` | String | `public` or `private` |
| `protocol` | String | `http1`, `h2c` |
| `cpuLimit` | int | millicores |
| `memoryLimit` | int | MB |
| `minScale` | int | |
| `maxScale` | int | |
| `hasEnvVars` | boolean | derived: `true` if any env vars are set |
| `createdAt` | String | |
| `updatedAt` | String | |

**`ScalewayFunction`**
| Field | Type | Notes |
|---|---|---|
| `id` | String | |
| `name` | String | |
| `namespaceId` | String | |
| `status` | String | |
| `region` | String | |
| `runtime` | String | e.g., `python311`, `node20`, `go122` |
| `privacy` | String | `public` or `private` |
| `memoryLimit` | int | MB |
| `cpuLimit` | int | millicores |
| `minScale` | int | |
| `maxScale` | int | |
| `hasEnvVars` | boolean | |
| `createdAt` | String | |
| `updatedAt` | String | |

Security focus: `WHERE privacy = 'public'` finds unauthenticated endpoints.
`WHERE hasEnvVars = true` flags potential plaintext secret storage — cross-reference with
Secret Manager to ensure secrets are fetched from there, not baked into env vars.

---

#### Block Storage

**`ScalewayVolume`**
| Field | Type | Notes |
|---|---|---|
| `id` | String | |
| `name` | String | |
| `volumeType` | String | `l_ssd`, `b_ssd` |
| `size` | long | bytes |
| `state` | String | `available`, `snapshotting`, `error`, etc. |
| `zone` | String | injected by client — not in API response directly |
| `projectId` | String | |
| `organizationId` | String | |
| `serverId` | String | nullable — `NULL` means unattached/orphaned |
| `createdAt` | String | |

Security focus: `WHERE serverId IS NULL` finds orphaned volumes — data at rest with no
active workload, often forgotten after server deletion.

**`ScalewaySnapshot`**
| Field | Type | Notes |
|---|---|---|
| `id` | String | |
| `name` | String | |
| `state` | String | `available`, `snapshotting`, `error`, etc. |
| `size` | long | bytes |
| `volumeType` | String | |
| `zone` | String | injected by client |
| `projectId` | String | |
| `organizationId` | String | |
| `tags` | List\<String\> | |
| `createdAt` | String | |

Security focus: filter by `createdAt <` a threshold date to find stale snapshots
holding old data beyond retention policy.

---

#### Load Balancers

**`ScalewayLoadBalancer`**
| Field | Type | Notes |
|---|---|---|
| `id` | String | |
| `name` | String | |
| `status` | String | `ready`, `pending`, `error`, etc. |
| `zone` | String | |
| `projectId` | String | |
| `organizationId` | String | |
| `tags` | List\<String\> | |
| `createdAt` | String | |
| `updatedAt` | String | |

**`ScalewayLoadBalancerFrontend`**
| Field | Type | Notes |
|---|---|---|
| `id` | String | |
| `name` | String | |
| `lbId` | String | parent LB |
| `zone` | String | |
| `protocol` | String | `http`, `https`, `tcp` |
| `inboundPort` | int | |
| `tlsEnabled` | boolean | derived: `true` if `protocol = https` or certificate IDs present |
| `createdAt` | String | |
| `updatedAt` | String | |

Note: `ScalewayLoadBalancerFrontend` is a dependent fetcher — requires `ScalewayLoadBalancer`
to be fetched first (iterates per LB to retrieve its frontends).

Security focus: `WHERE tlsEnabled = false AND protocol = 'http'` finds plaintext HTTP
frontends. Combined with `inboundPort = 80`, this identifies public unencrypted traffic.

---

#### Networking

**`ScalewayFlexibleIp`**
| Field | Type | Notes |
|---|---|---|
| `id` | String | |
| `ipAddress` | String | IPv4 address |
| `status` | String | `attached`, `detached`, `updating`, `error` |
| `zone` | String | |
| `projectId` | String | |
| `organizationId` | String | |
| `serverId` | String | nullable — `NULL` means unattached |
| `tags` | List\<String\> | |
| `createdAt` | String | |
| `updatedAt` | String | |

Security focus: `WHERE serverId IS NULL` finds unattached IPs that represent unnecessary
public IP address exposure and incur ongoing cost.

**`ScalewayVpc`**
| Field | Type | Notes |
|---|---|---|
| `id` | String | |
| `name` | String | |
| `region` | String | |
| `projectId` | String | |
| `organizationId` | String | |
| `defaultVpc` | boolean | `true` = auto-created default VPC |
| `routingEnabled` | boolean | routes traffic between all private networks in this VPC |
| `tags` | List\<String\> | |
| `createdAt` | String | |
| `updatedAt` | String | |

Security focus: `WHERE defaultVpc = true AND routingEnabled = true` is a lateral movement
risk — all private networks in the VPC can communicate freely.

**`ScalewayPrivateNetwork`**
| Field | Type | Notes |
|---|---|---|
| `id` | String | |
| `name` | String | |
| `vpcId` | String | parent VPC |
| `region` | String | |
| `projectId` | String | |
| `organizationId` | String | |
| `subnets` | List\<String\> | CIDR blocks, e.g., `["10.0.0.0/20"]` |
| `tags` | List\<String\> | |
| `createdAt` | String | |
| `updatedAt` | String | |

---

#### Audit Trail

**`ScalewayAuditEvent`**
| Field | Type | Notes |
|---|---|---|
| `id` | String | |
| `recordedAt` | String | event timestamp (renamed from `timestamp` — see Caveats) |
| `principalId` | String | user/app/policy that triggered the event |
| `principalType` | String | `user`, `application`, `assumed_role` |
| `sourceIp` | String | |
| `userAgent` | String | e.g., `terraform/1.7.0`, `scaleway-cli/2.20.0` |
| `apiMethod` | String | e.g., `iam.CreatePolicy` (renamed from `method` — see Caveats) |
| `status` | String | `success`, `forbidden`, `error` |
| `resourceType` | String | e.g., `instance.Server`, `iam.Policy` |
| `resourceId` | String | nullable |
| `projectId` | String | |
| `organizationId` | String | |
| `region` | String | |
| `locality` | String | nullable — zone for zone-scoped events |

Security focus queries:
- `WHERE status = 'forbidden'` — unauthorized access attempts
- `WHERE apiMethod LIKE 'iam.%'` — IAM changes (privilege escalation risk)
- `WHERE apiMethod LIKE '%Delete%' AND status = 'success'` — destructive actions
- `WHERE apiMethod LIKE 'keymanager.%'` — key deletion / rotation events
- `GROUP BY principalId` — activity volume per actor

---

#### Kubernetes (Kapsule)

**`ScalewayK8sCluster`**
| Field | Type | Notes |
|---|---|---|
| `id` | String | |
| `name` | String | |
| `status` | String | `ready`, `creating`, `deleting`, `error`, etc. |
| `version` | String | Kubernetes version, e.g., `1.30.2` |
| `cni` | String | `cilium`, `calico`, `flannel`, `weave` |
| `region` | String | |
| `projectId` | String | |
| `organizationId` | String | |
| `upgradeAvailable` | boolean | |
| `privateNetworkEnabled` | boolean | derived: `true` if `privateNetworkId` is set |
| `privateNetworkId` | String | nullable |
| `tags` | List\<String\> | |
| `createdAt` | String | |
| `updatedAt` | String | |

Security focus: `WHERE upgradeAvailable = true` finds clusters on outdated Kubernetes
versions. `WHERE privateNetworkEnabled = false AND status = 'ready'` finds clusters with
their API server potentially reachable from the public internet.

---

#### Container Registry

**`ScalewayRegistryNamespace`**
| Field | Type | Notes |
|---|---|---|
| `id` | String | |
| `name` | String | |
| `description` | String | |
| `status` | String | `ready`, `deleting`, `error`, etc. |
| `region` | String | |
| `projectId` | String | |
| `organizationId` | String | |
| `publiclyAccessible` | boolean | `true` = images pullable without auth (renamed from `isPublic` — see Caveats) |
| `imageCount` | int | |
| `size` | long | bytes |
| `endpoint` | String | registry URL |
| `createdAt` | String | |
| `updatedAt` | String | |

**`ScalewayRegistryImage`**
| Field | Type | Notes |
|---|---|---|
| `id` | String | |
| `name` | String | `{namespace}/{image}` |
| `namespaceId` | String | |
| `status` | String | `ready`, `deleting`, `error`, etc. |
| `visibility` | String | `public`, `private`, `inherit` (inherits namespace setting) |
| `region` | String | |
| `size` | long | bytes |
| `tags` | List\<String\> | image tags (e.g., `["v1.2.0", "latest"]`) |
| `createdAt` | String | |
| `updatedAt` | String | |

Security focus: `WHERE publiclyAccessible = true` on namespaces, or `WHERE visibility = 'public'`
on images. Images with empty `tags` are untagged/dangling.

---

#### Observability (Cockpit)

**`ScalewayCockpitAlertManager`** (one record per region)
| Field | Type | Notes |
|---|---|---|
| `region` | String | |
| `projectId` | String | |
| `managedAlertsEnabled` | boolean | Scaleway managed alert rules active |
| `contactPointCount` | int | number of notification channels configured |

Security focus: `WHERE managedAlertsEnabled = true AND contactPointCount = 0` finds regions
where alerts are configured but have nowhere to send notifications — a silent failure.
`WHERE managedAlertsEnabled = false` finds regions with no alerting at all.

---

#### Transactional Email (TEM)

**`ScalewayTemDomain`**
| Field | Type | Notes |
|---|---|---|
| `id` | String | |
| `name` | String | domain name, e.g., `email.mycompany.com` |
| `status` | String | `checked`, `unchecked`, `invalid`, `locked`, `revoked` |
| `region` | String | |
| `projectId` | String | |
| `organizationId` | String | |
| `spfConfigured` | boolean | derived: SPF DNS record present |
| `dkimConfigured` | boolean | derived: DKIM key present |
| `mxConfigured` | boolean | derived: MX record configured |
| `totalSent` | long | cumulative sent count |
| `totalFailed` | long | cumulative failure count |
| `createdAt` | String | |
| `updatedAt` | String | |

Security focus: `WHERE spfConfigured = false OR dkimConfigured = false` finds domains
susceptible to email spoofing. `WHERE status = 'revoked'` finds decommissioned domains
still registered. High `totalFailed / totalSent` ratio indicates a deliverability or
blacklisting problem.

---

### Dependent fetchers

Two DataFetchers require another resource type to be fetched first:

| Fetcher | Depends on | How |
|---|---|---|
| `ScalewaySecretVersionDataFetcher` | `ScalewaySecret` | iterates secrets, fetches versions per secret ID + region |
| `ScalewayLoadBalancerFrontendDataFetcher` | `ScalewayLoadBalancer` | iterates LBs, fetches frontends per LB ID + zone |

When supplying test data via `session.put()`, you can put the dependent type directly and
the parent fetch is skipped. When using live data, the parent type must be registered and
the `ScalewayClient` must be configured — both will be fetched automatically.

---

### Caveats and limitations

**SQL reserved keyword collisions with Apache Calcite**

`componentMethodConvention` strips the `is` prefix from boolean record accessors when
building column names. This means `boolean isPublic` becomes column `public`, and
`boolean isDefault` becomes column `default` — both SQL reserved words that Calcite
refuses to parse. During development several fields had to be renamed:

| API field | Java record field | Reason |
|---|---|---|
| `timestamp` | `recordedAt` | `TIMESTAMP` is a SQL reserved word |
| `method` | `apiMethod` | `METHOD` reserved in Calcite |
| `is_public` (Registry) | `publiclyAccessible` | avoids `is` stripping → `public` |
| `is_default` (VPC) | `defaultVpc` | avoids `is` stripping → `default` |

**Rule:** Never name a `boolean` field `isXxx` (the `is` prefix is stripped). Never use
a field name that is a SQL reserved word (`timestamp`, `method`, `type` as a standalone,
`default`, `public`, `group`, `order`, `select`, `from`, etc.).

**Volumes API returns a dict, not an array**

`GET /instance/v1/zones/{zone}/volumes` returns `{"volumes": {"<id>": {...}, ...}}` —
a map keyed by volume ID, not an array. The client handles this via `response.path("volumes").fields()`
and injects a synthetic `_zone` field into each node so the record can read the zone.
The same zone-injection pattern is used for snapshots.

**Audit Trail pagination can be very large**

`ScalewayAuditEvent` fetches all events across all regions. In active organizations this
can return tens of thousands of records. Consider filtering at the API level (the
`listAuditEvents()` method appends `organization_id` but not a time window). For production
use, extend `ScalewayClient.listAuditEvents()` to accept a `since` parameter.

**Cockpit API shape**

The Cockpit alertmanager endpoint returns a single JSON object (not paged). Contact points
return a `total_count` integer. If the Cockpit service is not enabled for a region, the
API returns a 404 which the client propagates as an `IOException`. Wrap the fetcher call
in a try-catch if you want graceful degradation.

**No SDK — raw HTTP**

There is no official Scaleway Java SDK. All calls use `java.net.http.HttpClient` with
Jackson for JSON parsing. This means API changes (new fields, renamed keys) require
manual updates to `from(JsonNode)` factory methods.

**All timestamps are strings**

Dates and times are stored as ISO-8601 strings (`String`), not `java.time.Instant` or
`OffsetDateTime`. String comparison works for ISO-8601 (`WHERE createdAt < '2024-01-01'`)
but there is no date arithmetic in queries.

**`ScalewayIamApiKey` has no `id` field**

The primary key for API keys is `accessKey` (the public access key identifier), not `id`.
Do not expect an `id` column when querying `ScalewayIamApiKey`.

**`ScalewaySecurityGroupRule` ports are nullable `Integer`**

`destPortFrom` and `destPortTo` are `Integer` (boxed), not `int`. They are `NULL` when
the rule applies to all ports (e.g., ICMP or ANY protocol rules). Use `IS NULL` to find
open-all-ports rules: `WHERE destPortFrom IS NULL AND ipRange = '0.0.0.0/0'`.

---

### Key security & compliance query patterns

```sql
-- Users without MFA
SELECT u.id, u.email, u.lastLoginAt
FROM ScalewayIamUser u
WHERE u.mfa = false AND u.status = 'activated'

-- Non-expiring API keys
SELECT k.accessKey, k.userId, k.applicationId, k.createdAt
FROM ScalewayIamApiKey k
WHERE k.expiresAt IS NULL

-- Overly broad IAM policies (> 5 permission sets)
SELECT p.id, p.name, p.nbPermissionSets, p.nbScopes
FROM ScalewayIamPolicy p
WHERE p.nbPermissionSets > 5

-- Instances exposed to internet without deletion protection
SELECT i.id, i.name, i.publicIp, i.zone
FROM ScalewayInstance i
WHERE i.publicIp IS NOT NULL AND i.instanceProtected = false

-- Security groups accepting all inbound traffic by default
SELECT sg.id, sg.name, sg.zone
FROM ScalewaySecurityGroup sg
WHERE sg.inboundDefaultPolicy = 'accept'

-- Inbound rules open to the internet
SELECT r.id, r.securityGroupId, r.protocol, r.destPortFrom, r.destPortTo
FROM ScalewaySecurityGroupRule r
WHERE r.ipRange = '0.0.0.0/0' AND r.direction = 'inbound' AND r.action = 'accept'

-- Databases publicly accessible or lacking HA / backup
SELECT d.id, d.name, d.engine, d.region, d.publiclyAccessible, d.haEnabled, d.backupRetentionDays
FROM ScalewayDatabase d
WHERE d.publiclyAccessible = true OR d.backupRetentionDays < 7 OR d.haEnabled = false

-- KMS keys with no rotation policy
SELECT k.id, k.name, k.algorithm, k.region
FROM ScalewayKmsKey k
WHERE k.rotationEnabled = false AND k.state = 'enabled'

-- Public serverless endpoints
SELECT c.id, c.name, c.region FROM ScalewayContainer c WHERE c.privacy = 'public'
UNION ALL
SELECT f.id, f.name, f.region FROM ScalewayFunction f WHERE f.privacy = 'public'

-- Orphaned volumes (unattached)
SELECT v.id, v.name, v.size, v.zone
FROM ScalewayVolume v
WHERE v.serverId IS NULL

-- Load balancer frontends without TLS
SELECT f.id, f.name, f.lbId, f.inboundPort, f.zone
FROM ScalewayLoadBalancerFrontend f
WHERE f.tlsEnabled = false AND f.protocol = 'http'

-- Default VPCs with routing enabled (lateral movement risk)
SELECT v.id, v.name, v.region
FROM ScalewayVpc v
WHERE v.defaultVpc = true AND v.routingEnabled = true

-- Audit: forbidden access attempts in last 30 days
SELECT e.recordedAt, e.apiMethod, e.principalId, e.principalType, e.sourceIp
FROM ScalewayAuditEvent e
WHERE e.status = 'forbidden'

-- Audit: IAM privilege escalation activity
SELECT e.recordedAt, e.apiMethod, e.principalId, e.sourceIp
FROM ScalewayAuditEvent e
WHERE e.apiMethod LIKE 'iam.%' AND e.status = 'success'

-- K8s clusters with upgrades available or no private network
SELECT c.id, c.name, c.version, c.region, c.upgradeAvailable, c.privateNetworkEnabled
FROM ScalewayK8sCluster c
WHERE c.upgradeAvailable = true OR c.privateNetworkEnabled = false

-- Public container registry namespaces
SELECT n.id, n.name, n.endpoint, n.region
FROM ScalewayRegistryNamespace n
WHERE n.publiclyAccessible = true

-- Email domains missing SPF or DKIM
SELECT d.id, d.name, d.status, d.spfConfigured, d.dkimConfigured
FROM ScalewayTemDomain d
WHERE d.spfConfigured = false OR d.dkimConfigured = false

-- Regions with alerting enabled but no contact points
SELECT a.region, a.contactPointCount
FROM ScalewayCockpitAlertManager a
WHERE a.managedAlertsEnabled = true AND a.contactPointCount = 0
```
